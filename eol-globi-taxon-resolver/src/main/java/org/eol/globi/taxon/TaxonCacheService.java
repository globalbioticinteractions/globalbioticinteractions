package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaxonCacheService extends CacheService implements PropertyEnricher, TermMatcher {
    private static final Log LOG = LogFactory.getLog(TaxonCacheService.class);

    private BTreeMap<String, Map<String, String>> resolvedIdToTaxonMap = null;
    private BTreeMap<String, String> providedToResolvedMap = null;
    private BTreeMap<String, Set<String>> providedToResolvedMaps = null;
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

    public Map<String, String> getTaxon(String value) {
        Map<String, String> enriched = null;
        if (TaxonUtil.isNonEmptyValue(value)) {
            Set<String> ids = providedToResolvedMaps.get(value);
            if (ids != null && !ids.isEmpty()) {
                String resolvedId = ids.iterator().next();
                enriched = resolvedIdToTaxonMap.get(resolvedId);
            }
        }
        return enriched;
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
        if (resolvedIdToTaxonMap == null || providedToResolvedMaps == null) {
            init();
        }
    }

    public void init() throws PropertyEnricherException {
        LOG.info("taxon cache initializing...");
        LOG.info("taxon cache loading [" + taxonCacheResource + "]...");
        DB db = initDb("taxonCache");

        StopWatch watch = new StopWatch();
        watch.start();
        try {
            resolvedIdToTaxonMap = db
                    .createTreeMap("taxonCacheById")
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
        LOG.info("taxon map loading [" + taxonMapResource + "] ...");
        watch.start();
        try {
            providedToResolvedMaps = db
                    .createTreeMap("taxonMappingById")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            BufferedReader reader = createBufferedReader(taxonMapResource);
            final LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledTSVParser(reader);
            while (labeledCSVParser.getLine() != null) {
                Taxon provided = TaxonMapParser.parseProvidedTaxon(labeledCSVParser);
                Taxon resolved = TaxonMapParser.parseResolvedTaxon(labeledCSVParser);
                addIfNeeded(providedToResolvedMaps, provided.getExternalId(), resolved.getExternalId());
                addIfNeeded(providedToResolvedMaps, provided.getName(), resolved.getExternalId());
                addIfNeeded(providedToResolvedMaps, resolved.getName(), resolved.getExternalId());
            }

        } catch (IOException e) {
            throw new PropertyEnricherException("failed to build taxon cache map", e);
        }
        watch.stop();
        logCacheLoadStats(watch.getTime(), providedToResolvedMaps.size());
        LOG.info("taxon map loading [" + taxonMapResource + "] done.");

        LOG.info("taxon cache initialized.");
    }

    public void addIfNeeded(BTreeMap<String, Set<String>> providedToResolvedIds, String providedKey, String resolvedId) {
        if (StringUtils.isNotBlank(providedKey) && StringUtils.isNotBlank(resolvedId)) {
            Set<String> someIds = providedToResolvedIds.get(providedKey);
            if (someIds == null) {
                someIds = new TreeSet<>();
            }
            someIds.add(resolvedId);
            providedToResolvedIds.put(providedKey, someIds);
        }
    }

    @Override
    public void findTermsForNames(List<String> names, TermMatchListener termMatchListener, List<GlobalNamesSources> sources) throws PropertyEnricherException {
        Stream<Term> namesAndIds = names.stream().flatMap(name -> Stream.of(new TermImpl(null, name)));
        findTerms(namesAndIds.collect(Collectors.toList()), termMatchListener, sources);
    }

    @Override
    public void findTerms(List<Term> terms, TermMatchListener termMatchListener, List<GlobalNamesSources> sources) throws PropertyEnricherException {
        lazyInit();
        for (Term term : terms) {
            if (!resolveName(termMatchListener, term.getId())) {
                if (!resolveName(termMatchListener, term.getName())) {
                    termMatchListener.foundTaxonForName(null, term.getName(), new TaxonImpl(term.getName(), term.getId()), NameType.NONE);
                }
            }
        }
    }

    private boolean resolveName(TermMatchListener termMatchListener, String name) {
        boolean hasResolved = false;

        if (StringUtils.isNotBlank(name)) {
            Set<String> ids = providedToResolvedMaps.get(name);

            if (ids != null) {
                for (String resolvedId : ids) {
                    Map<String, String> resolved = resolvedIdToTaxonMap.get(resolvedId);
                    if (resolved != null) {
                        Taxon resolvedTaxon = TaxonUtil.mapToTaxon(resolved);
                        termMatchListener.foundTaxonForName(null, name, resolvedTaxon, NameType.SAME_AS);
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
        if (providedToResolvedMap != null) {
            close(providedToResolvedMap.getEngine());
            resolvedIdToTaxonMap = null;
        }
    }

    static public void close(Engine engine) {
        if (!engine.isClosed()) {
            engine.close();
        }
    }

}
