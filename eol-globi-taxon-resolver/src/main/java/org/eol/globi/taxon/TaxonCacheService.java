package org.eol.globi.taxon;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.lucene.store.SimpleFSDirectory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.CacheServiceUtil;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.TermRequestImpl;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.Engine;
import org.mapdb.Fun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TaxonCacheService extends CacheService implements PropertyEnricher, TermMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonCacheService.class);

    private BTreeMap<String, Map<String, String>> resolvedIdToTaxonMap = null;

    private TaxonLookupServiceImpl taxonLookupService = null;

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
    public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
        List<Map<String, String>> enriched = enrichAllMatches(properties);
        return (enriched == null || enriched.size() == 0)
                ? Collections.unmodifiableMap(properties)
                : enriched.get(0);
    }

    @Override
    public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
        lazyInit();
        String externalId = getExternalId(properties);
        List<Map<String, String>> enriched = null;
        if (StringUtils.isNotBlank(externalId)) {
            enriched = getTaxon(externalId);
        }
        if (enriched == null) {
            enriched = getTaxon(getName(properties));
        }
        return enriched;
    }

    private List<Map<String, String>> getTaxon(String value) throws PropertyEnricherException {
        List<Map<String, String>> enriched = null;
        if (TaxonUtil.isNonEmptyValue(value)) {
            Taxon[] taxaMatched = lookupTerm(value);
            for (Taxon taxonMatch : taxaMatched) {
                String resolvedId = taxonMatch.getExternalId();
                Map<String, String> enrichedSingle = resolvedIdToTaxonMap.get(StringUtils.lowerCase(resolvedId));
                if (enrichedSingle != null) {
                    if (enriched == null) {
                        enriched = new ArrayList<>();
                    }
                    enriched.add(enrichedSingle);
                }

            }
        }
        return enriched;
    }

    private Taxon[] lookupTerm(String value) throws PropertyEnricherException {
        Taxon[] ids;
        try {
            ids = taxonLookupService.lookupTermsByName(StringUtils.lowerCase(value));
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

    private void lazyInit() {
        if (resolvedIdToTaxonMap == null || taxonLookupService == null) {
            init();
        }
    }

    private void init() {
        try {
            initTaxonCache();
            initTaxonIdMap();
        } catch (IOException ex) {
            throw new IllegalStateException("problem initiating taxon cache index", ex);
        }
    }

    private void initTaxonIdMap() throws IOException {
            Path luceneDir = Paths.get(getCacheDir().getAbsolutePath(), "lucene");
            if (!luceneDir.toFile().exists()) {
                buildIndex(luceneDir);
            }
            this.taxonLookupService = new TaxonLookupServiceImpl(new SimpleFSDirectory(luceneDir)) {{
                setMaxHits(getMaxTaxonLinks());
            }};

    }

    private void buildIndex(Path luceneDir) throws IOException {
        Path tmpLuceneDir = Paths.get(getCacheDir().getAbsolutePath(), "lucene" + UUID.randomUUID());
        CacheServiceUtil.createCacheDir(tmpLuceneDir.toFile());
        SimpleFSDirectory indexDir = new SimpleFSDirectory(tmpLuceneDir);
        TaxonLookupBuilder taxonLookupService = new TaxonLookupBuilder(indexDir) {{
            start();
        }};
        final AtomicInteger count = new AtomicInteger(0);
        LOG.info("local taxon map of [" + taxonMap.getResource() + "] building...");

        StopWatch watch = new StopWatch();
        watch.start();
        BufferedReader reader = CacheServiceUtil.createBufferedReader(taxonMap.getResource());

        reader.lines()
                .filter(taxonMap.getValidator())
                .map(taxonMap.getParser())
                .forEach(triple -> {
                    addIfNeeded(taxonLookupService, triple.getLeft().getExternalId(), triple.getRight().getExternalId());
                    addIfNeeded(taxonLookupService, triple.getLeft().getName(), triple.getRight().getExternalId());
                    addIfNeeded(taxonLookupService, triple.getRight().getExternalId(), triple.getRight().getExternalId());
                    addIfNeeded(taxonLookupService, triple.getRight().getName(), triple.getRight().getExternalId());
                    count.incrementAndGet();
                });
        watch.stop();
        logCacheLoadStats(watch.getTime(), count.get());
        LOG.info("local taxon map of [" + taxonMap.getResource() + "] built.");
        watch.reset();
        taxonLookupService.finish();
        try {
            FileUtils.moveDirectory(tmpLuceneDir.toFile(), luceneDir.toFile());
        } catch (FileExistsException ex) {
            LOG.info("failed to move recently built index at [" + tmpLuceneDir.toFile().getAbsolutePath() + "] to [" + luceneDir.toFile().getAbsolutePath() + "]. Assuming that some other builder has already created the index.");
            FileUtils.deleteDirectory(tmpLuceneDir.toFile());
        }
    }

    private void initTaxonCache() throws IOException {
        DB db = initDb("taxonCache");
        String taxonCacheName = "taxonCacheById";
        if (db.exists(taxonCacheName)) {
            resolvedIdToTaxonMap = db.getTreeMap(taxonCacheName);
        } else {
            LOG.info("local taxon cache of [" + taxonCache.getResource() + "] building...");
            StopWatch watch = new StopWatch();
            watch.start();
            String tmpTaxonCacheName = "taxonCacheById" + UUID.randomUUID();
            BTreeMap<String, Map<String, String>> tmpResolvedIdToTaxonMap = null;
            try {
                tmpResolvedIdToTaxonMap = db
                        .createTreeMap(tmpTaxonCacheName)
                        .pumpPresort(100000)
                        .pumpIgnoreDuplicates()
                        .pumpSource(taxonCacheIterator(taxonCache))
                        .keySerializer(BTreeKeySerializer.STRING)
                        .make();
                db.commit();
            } catch (IOException e) {
                throw new IllegalStateException("failed to instantiate taxonCache: [" + e.getMessage() + "]", e);
            }
            watch.stop();
            logCacheLoadStats(watch.getTime(), tmpResolvedIdToTaxonMap.size());
            watch.reset();
            if (db.exists(taxonCacheName)) {
                LOG.info("another local taxon cache of [" + taxonCache.getResource() + "] was created during index creation, dropping built index.");
                db.delete(tmpTaxonCacheName);
            } else {
                resolvedIdToTaxonMap = tmpResolvedIdToTaxonMap;
                db.rename(tmpTaxonCacheName, taxonCacheName);
                LOG.info("local taxon cache of [" + taxonCache.getResource() + "] built.");
            }
        }
    }

    private void addIfNeeded(TaxonImportListener lookupService, String providedKey, String resolvedId) {
        if (TaxonUtil.isNonEmptyValue(providedKey) && TaxonUtil.isNonEmptyValue(resolvedId)) {
            lookupService.addTerm(StringUtils.lowerCase(providedKey), new TaxonImpl(null, resolvedId));
        }
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        lazyInit();
        for (Term term : terms) {
            String nodeIdAndName = term.getName();
            Long nodeId = term instanceof TermRequestImpl ? ((TermRequestImpl) term).getNodeId() : null;
            if (!resolveName(termMatchListener, term, term.getId(), nodeId)) {
                if (StringUtils.isBlank(nodeIdAndName) || !resolveName(termMatchListener, term, term.getName(), nodeId)) {
                    termMatchListener.foundTaxonForTerm(nodeId, term, new TaxonImpl(term.getId(), term.getName()), NameType.NONE);
                }
            }
        }
    }

    private boolean resolveName(TermMatchListener termMatchListener, Term term, String name, Long nodeId) throws PropertyEnricherException {
        boolean hasResolved = false;
        if (StringUtils.isNotBlank(name)) {
            Taxon[] ids = lookupTerm(name);
            if (ids != null) {
                List<String> idsDistinct = Arrays.stream(ids)
                        .filter(t -> StringUtils.isNotBlank(t.getExternalId()))
                        .map(Taxon::getExternalId)
                        .map(StringUtils::lowerCase)
                        .distinct()
                        .limit(getMaxTaxonLinks())
                        .collect(Collectors.toList());
                for (String resolvedId : idsDistinct) {
                    Map<String, String> resolved = resolvedIdToTaxonMap.get(resolvedId);
                    if (resolved != null) {
                        Taxon resolvedTaxon = TaxonUtil.mapToTaxon(resolved);
                        termMatchListener.foundTaxonForTerm(nodeId, term, resolvedTaxon, NameType.SAME_AS);
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
        return TaxonUtil.isNonEmptyValue(value) ? StringUtils.lowerCase(value) : PropertyAndValueDictionary.NO_MATCH;
    }

    static public Iterator<Fun.Tuple2<String, Map<String, String>>> taxonCacheIterator(final TermResource<Taxon> config) throws IOException {

        return new Iterator<Fun.Tuple2<String, Map<String, String>>>() {
            private BufferedReader reader = CacheServiceUtil.createBufferedReader(config.getResource());
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
                while (!lineAvailable.get() && (currentLine = reader.readLine()) != null) {
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

    public static void logCacheLoadStats(long time, int numberOfItems, Logger log) {
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
            try {
                taxonLookupService.close();
            } catch (IOException e) {
                // ignore
            }
            taxonLookupService = null;
        }
    }

    static public void close(Engine engine) {
        if (!engine.isClosed()) {
            engine.close();
        }
    }

}
