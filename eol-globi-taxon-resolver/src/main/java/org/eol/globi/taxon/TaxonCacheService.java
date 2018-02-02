package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaxonCacheService extends CacheService implements PropertyEnricher, TermMatcher {
    private static final Log LOG = LogFactory.getLog(TaxonCacheService.class);

    private BTreeMap<String, Map<String, String>> resolvedIdToTaxonMap = null;

    private TaxonLookupService taxonLookupService = null;

    private String taxonCacheResource;
    private final String taxonMapResource;

    public TaxonCacheService(String taxonCacheResource, String taxonMapResource) {
        this.taxonCacheResource = taxonCacheResource;
        this.taxonMapResource = taxonMapResource;
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

    public void lazyInit() throws PropertyEnricherException {
        if (resolvedIdToTaxonMap == null || taxonLookupService == null) {
            init();
        }
    }

    public void init() throws PropertyEnricherException {
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
            TaxonLookupServiceImpl taxonLookupService = new TaxonLookupServiceImpl(new SimpleFSDirectory(luceneDir));
            taxonLookupService.start();
            if (!isTemporary() && preexisting) {
                LOG.info("pre-existing taxon lookup index found, no need to re-index...");
            } else {
                LOG.info("no pre-existing taxon lookup index found, re-indexing...");
                int count = 0;
                LOG.info("taxon map loading [" + taxonMapResource + "] ...");

                StopWatch watch = new StopWatch();
                watch.start();
                BufferedReader reader = createBufferedReader(taxonMapResource);
                final LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledTSVParser(reader);
                while (labeledCSVParser.getLine() != null) {
                    Taxon provided = TaxonMapParser.parseProvidedTaxon(labeledCSVParser);
                    Taxon resolved = TaxonMapParser.parseResolvedTaxon(labeledCSVParser);
                    addIfNeeded(taxonLookupService, provided.getExternalId(), resolved.getExternalId());
                    addIfNeeded(taxonLookupService, provided.getName(), resolved.getExternalId());
                    addIfNeeded(taxonLookupService, resolved.getName(), resolved.getExternalId());
                    count++;
                }
                watch.stop();
                logCacheLoadStats(watch.getTime(), count);
                LOG.info("taxon map loading [" + taxonMapResource + "] done.");
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
            LOG.info("taxon cache loading [" + taxonCacheResource + "]...");
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                resolvedIdToTaxonMap = db
                        .createTreeMap(taxonCacheName)
                        .pumpPresort(100000)
                        .pumpIgnoreDuplicates()
                        .pumpSource(taxonCacheIterator(taxonCacheResource, new LineSkipper() {
                            @Override
                            public boolean shouldSkipLine(LabeledCSVParser parser) {
                                final Taxon taxon = TaxonCacheParser.parseLine(parser);
                                return StringUtils.isBlank(taxon.getPath());
                            }
                        }))
                        .keySerializer(BTreeKeySerializer.STRING)
                        .make();
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to instantiate taxonCache: [" + e.getMessage() + "]", e);
            }
            watch.stop();
            LOG.info("taxon cache loading [" + taxonCacheResource + "] done.");
            logCacheLoadStats(watch.getTime(), resolvedIdToTaxonMap.size());
            watch.reset();
        }
    }

    public void addIfNeeded(TaxonLookupServiceImpl lookupService, String providedKey, String resolvedId) {
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
            String[] split = StringUtils.split(nodeIdAndName, '|');
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
                        .map(Taxon::getExternalId).distinct().collect(Collectors.toList());
                for(String resolvedId : idsDistinct) {
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

    interface LineSkipper {
        boolean shouldSkipLine(LabeledCSVParser parser);
    }

    static private String valueOrNoMatch(String value) {
        return TaxonUtil.isNonEmptyValue(value) ? value : PropertyAndValueDictionary.NO_MATCH;
    }

    static public Iterator<Fun.Tuple2<String, Map<String, String>>> taxonCacheIterator(final String resource, final LineSkipper skipper) throws IOException {

        return new Iterator<Fun.Tuple2<String, Map<String, String>>>() {
            private BufferedReader reader = createBufferedReader(resource);
            private final LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledTSVParser(reader);
            private AtomicBoolean lineReady = new AtomicBoolean(false);

            @Override
            public boolean hasNext() {
                try {
                    boolean hasNext;
                    do {
                        hasNext = lineReady.get() || consumeLine(labeledCSVParser);
                    } while (hasNext && skipper.shouldSkipLine(labeledCSVParser));

                    return hasNext;
                } catch (IOException e) {
                    LOG.error("failed to get next line", e);
                    return false;
                }
            }

            private boolean consumeLine(LabeledCSVParser labeledCSVParser) throws IOException {
                boolean hasNext = labeledCSVParser.getLine() != null;
                if (skipper.shouldSkipLine(labeledCSVParser)) {
                    lineReady.set(false);
                } else {
                    lineReady.set(hasNext);
                }
                return hasNext;
            }

            @Override
            public Fun.Tuple2<String, Map<String, String>> next() {
                final Taxon taxon = TaxonCacheParser.parseLine(labeledCSVParser);
                lineReady.set(false);
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
