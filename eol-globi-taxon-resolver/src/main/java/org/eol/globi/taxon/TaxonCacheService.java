package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.Engine;
import org.mapdb.Fun;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaxonCacheService extends CacheService implements PropertyEnricher, TermMatcher {
    private static final Log LOG = LogFactory.getLog(TaxonCacheService.class);

    private BTreeMap<String, Map<String, String>> resolvedIdToTaxonMap = null;

    private TaxonLookupService taxonLookupService = null;


    // maximum number of expected taxon links related to a given taxon id
    private int maxTaxonLinks = 125;

    private final TermResource<Taxon> taxonCache;
    private final TermResource<Triple<Taxon, NameType, Taxon>> taxonMap;

    public TaxonCacheService(final TermResource<Taxon> taxonCache, final TermResource<Triple<Taxon, NameType, Taxon>> taxonMap) {
        this.taxonCache = taxonCache;
        this.taxonMap = taxonMap;
    }

    public TaxonCacheService(final String termResource, final String taxonMapResource) {
        this(TermResources.defaultTaxonCacheResource(termResource), TermResources.defaultTaxonMapResource(taxonMapResource));
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        lazyInit();
        String externalId = getExternalId(properties);
        Map<String, String> enriched = null;
        if (StringUtils.isNotBlank(externalId)) {
            enriched = getTaxon(externalId);
        }
        if (enriched == null) {
            enriched = getTaxon(getName(properties));
        }
        return enriched == null ? Collections.unmodifiableMap(properties) : enriched;
    }

    private Map<String, String> getTaxon(String value) throws PropertyEnricherException {
        Map<String, String> enriched = null;
        if (TaxonUtil.isNonEmptyValue(value)) {
            Taxon[] ids = lookupTerm(value);
            if (ids != null && ids.length > 0) {
                String resolvedId = ids[0].getExternalId();
                enriched = resolvedIdToTaxonMap.get(resolvedId);
            }
        }
        return enriched;
    }

    private Taxon[] lookupTerm(String value) throws PropertyEnricherException {
        Taxon[] ids;
        try {
            ids = taxonLookupService.lookupTermsByName(value);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + value + "]", e);
        }
        return ids;
    }

    public String getName(Map<String, String> properties) {
        String name = null;
        if (properties.containsKey(PropertyAndValueDictionary.NAME)) {
            name = properties.get(PropertyAndValueDictionary.NAME);
        }
        return name;
    }

    public String getExternalId(Map<String, String> properties) {
        String externalId = null;
        if (properties.containsKey(PropertyAndValueDictionary.EXTERNAL_ID)) {
            externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        }
        return externalId;
    }

    private void lazyInit() throws PropertyEnricherException {
        if (resolvedIdToTaxonMap == null || taxonLookupService == null) {
            init();
        }
    }

    private void init() throws PropertyEnricherException {
        LOG.info("taxon cache initializing at [" + getCacheDir().getAbsolutePath() + "...");

        initTaxonCache();
        initTaxonIdMap();

        LOG.info("taxon cache at [" + getCacheDir().getAbsolutePath() + " initialized.");
    }

    private void initTaxonIdMap() throws PropertyEnricherException {
        try {
            LOG.info("taxon lookup service instantiating...");
            File luceneDir = new File(getCacheDir().getAbsolutePath(), "lucene");
            boolean preexisting = luceneDir.exists();
            createCacheDir(luceneDir, isTemporary());
            TaxonLookupServiceImpl taxonLookupService = new TaxonLookupServiceImpl(new SimpleFSDirectory(luceneDir)) {{
                setMaxHits(getMaxTaxonLinks());
                start();
            }};
            if (!isTemporary() && preexisting) {
                LOG.info("pre-existing taxon lookup index found, no need to re-index...");
            } else {
                LOG.info("no pre-existing taxon lookup index found, re-indexing...");
                final AtomicInteger count = new AtomicInteger(0);
                LOG.info("taxon map loading [" + taxonMap.getResource() + "] ...");

                StopWatch watch = new StopWatch();
                watch.start();
                BufferedReader reader = createBufferedReader(taxonMap.getResource());

                reader.lines()
                        .filter(taxonMap.getValidator())
                        .map(taxonMap.getParser())
                        .forEach(triple -> {
                            addIfNeeded(taxonLookupService, triple.getLeft().getExternalId(), triple.getRight().getExternalId());
                            addIfNeeded(taxonLookupService, triple.getLeft().getName(), triple.getRight().getExternalId());
                            addIfNeeded(taxonLookupService, triple.getRight().getName(), triple.getRight().getExternalId());
                            count.incrementAndGet();
                        });
                watch.stop();
                logCacheLoadStats(watch.getTime(), count.get());
                LOG.info("taxon map loading [" + taxonMap.getResource() + "] done.");
            }
            taxonLookupService.finish();
            this.taxonLookupService = taxonLookupService;
            LOG.info("taxon lookup service instantiating done.");

        } catch (IOException e) {
            throw new PropertyEnricherException("problem initiating taxon cache index", e);
        }
    }

    private void initTaxonCache() throws PropertyEnricherException {
        DB db = initDb("taxonCache");

        String taxonCacheName = "taxonCacheById";
        if (db.exists(taxonCacheName)) {
            LOG.info("re-using pre-existing cache");
            resolvedIdToTaxonMap = db.getTreeMap(taxonCacheName);
        } else {
            LOG.info("no pre-existing cache found, rebuilding...");
            LOG.info("taxon cache loading [" + taxonCache.getResource() + "]...");
            StopWatch watch = new StopWatch();
            watch.start();

            try {
                resolvedIdToTaxonMap = db
                        .createTreeMap(taxonCacheName)
                        .pumpPresort(100000)
                        .pumpIgnoreDuplicates()
                        .pumpSource(taxonCacheIterator(taxonCache))
                        .keySerializer(BTreeKeySerializer.STRING)
                        .make();
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to instantiate taxonCache: [" + e.getMessage() + "]", e);
            }
            watch.stop();
            LOG.info("taxon cache loading [" + taxonCache.getResource() + "] done.");
            logCacheLoadStats(watch.getTime(), resolvedIdToTaxonMap.size());
            watch.reset();
        }
    }

    private void addIfNeeded(TaxonImportListener lookupService, String providedKey, String resolvedId) {
        if (TaxonUtil.isNonEmptyValue(providedKey) && TaxonUtil.isNonEmptyValue(resolvedId)) {
            lookupService.addTerm(providedKey, new TaxonImpl(null, resolvedId));
        }
    }

    @Override
    public void findTermsForNames(List<String> names, TermMatchListener termMatchListener) throws PropertyEnricherException {
        Stream<Term> namesAndIds = names.stream()
                .flatMap(name -> Stream.of(new TermImpl(null, name)));
        findTerms(namesAndIds.collect(Collectors.toList()), termMatchListener);
    }

    @Override
    public void findTerms(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        lazyInit();
        for (Term term : terms) {
            String nodeIdAndName = term.getName();
            String[] split = CSVTSVUtil.splitPipes(nodeIdAndName);
            String name = (split != null && split.length > 1) ? split[1] : nodeIdAndName;
            Long nodeId = (split != null && split.length > 1 && NumberUtils.isDigits(split[0])) ? Long.parseLong(split[0]) : null;
            if (!resolveName(termMatchListener, term.getId(), nodeId)) {
                if (StringUtils.isNotBlank(nodeIdAndName)) {
                    if (!resolveName(termMatchListener, name, nodeId)) {
                        termMatchListener.foundTaxonForName(nodeId, name, new TaxonImpl(name, term.getId()), NameType.NONE);
                    }
                }
            }
        }
    }

    private boolean resolveName(TermMatchListener termMatchListener, String name, Long nodeId) throws PropertyEnricherException {
        boolean hasResolved = false;
        if (StringUtils.isNotBlank(name)) {
            Taxon[] ids = lookupTerm(name);
            if (ids != null) {
                List<String> idsDistinct = Arrays.stream(ids)
                        .filter(t -> StringUtils.isNotBlank(t.getExternalId()))
                        .map(Taxon::getExternalId)
                        .distinct()
                        .limit(getMaxTaxonLinks())
                        .collect(Collectors.toList());
                for (String resolvedId : idsDistinct) {
                    Map<String, String> resolved = resolvedIdToTaxonMap.get(resolvedId);
                    if (resolved != null) {
                        Taxon resolvedTaxon = TaxonUtil.mapToTaxon(resolved);
                        termMatchListener.foundTaxonForName(nodeId, name, resolvedTaxon, NameType.SAME_AS);
                        hasResolved = true;
                    }
                }
            }
        }
        return hasResolved;
    }

    public int getMaxTaxonLinks() {
        return maxTaxonLinks;
    }

    public void setMaxTaxonLinks(int maxTaxonLinks) {
        this.maxTaxonLinks = maxTaxonLinks;
    }

    static private String valueOrNoMatch(String value) {
        return TaxonUtil.isNonEmptyValue(value) ? value : PropertyAndValueDictionary.NO_MATCH;
    }

    static public Iterator<Fun.Tuple2<String, Map<String, String>>> taxonCacheIterator(final TermResource<Taxon> config) throws IOException {

        return new Iterator<Fun.Tuple2<String, Map<String, String>>>() {
            private BufferedReader reader = createBufferedReader(config.getResource());
            private AtomicBoolean lineAvailable = new AtomicBoolean(false);
            private String currentLine = null;

            @Override
            public boolean hasNext() {
                try {
                    return proceedToNextValidLine();
                } catch (IOException e) {
                    LOG.error("failed to get next line", e);
                    return false;
                }
            }

            private boolean proceedToNextValidLine() throws IOException {
                while(!lineAvailable.get() && (currentLine = reader.readLine()) != null) {
                    if (config.getValidator().test(currentLine)) {
                        lineAvailable.set(true);
                    }
                }
                return lineAvailable.get();
            }

            @Override
            public Fun.Tuple2<String, Map<String, String>> next() {
                final Taxon taxon = config.getParser().apply(currentLine);
                lineAvailable.set(false);
                return new Fun.Tuple2<>(valueOrNoMatch(taxon.getExternalId()), TaxonUtil.taxonToMap(taxon));
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    static public void logCacheLoadStats(long time, int numberOfItems) {
        logCacheLoadStats(time, numberOfItems, LOG);
    }

    public static void logCacheLoadStats(long time, int numberOfItems, Log log) {
        final double avgRate = numberOfItems * 1000.0 / time;
        final double timeElapsedInSeconds = time / 1000.0;
        final String msg = String.format("cache with [%d]" + " items built in [%.1f] s or [%.1f] items/s.",
                numberOfItems,
                timeElapsedInSeconds,
                avgRate);
        log.info(msg);
    }

    @Override
    public void shutdown() {
        if (resolvedIdToTaxonMap != null) {
            close(resolvedIdToTaxonMap.getEngine());
            resolvedIdToTaxonMap = null;
        }
        if (taxonLookupService != null) {
            taxonLookupService.destroy();
            taxonLookupService = null;
        }
    }

    static public void close(Engine engine) {
        if (!engine.isClosed()) {
            engine.close();
        }
    }

}
