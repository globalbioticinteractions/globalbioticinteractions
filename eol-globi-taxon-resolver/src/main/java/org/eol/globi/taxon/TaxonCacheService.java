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
            throw new PropertyEnricherException("failed to instantiate taxonCache", e);
        }
        watch.stop();
        logCacheLoadStats(watch.getTime(), resolvedIdToTaxonMap.size());
        watch.reset();
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
        lazyInit();
        for (String name : names) {
            if (StringUtils.isNotBlank(name)) {
                Set<String> ids = providedToResolvedMaps.get(name);

                if (ids != null) {
                    for (String resolvedId : ids) {
                        Map<String, String> resolved = resolvedIdToTaxonMap.get(resolvedId);
                        if (resolved != null) {
                            Taxon resolvedTaxon = TaxonUtil.mapToTaxon(resolved);
                            termMatchListener.foundTaxonForName(null, name, resolvedTaxon, NameType.SAME_AS);
                        }
                    }
                }
            }
        }

    }

    private enum ProcessingState {
        PROVIDED_NAME,
        PROVIDED_ID,
        RESOLVED_NAME,
        RESOLVED_ID,
        DONE
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

            @Override
            public boolean hasNext() {
                try {
                    boolean hasNext;
                    do {
                        hasNext = labeledCSVParser.getLine() != null;
                    } while (hasNext && skipper.shouldSkipLine(labeledCSVParser));

                    return hasNext;
                } catch (IOException e) {
                    LOG.error("failed to get next line", e);
                    return false;
                }
            }

            @Override
            public Fun.Tuple2<String, Map<String, String>> next() {
                final Taxon taxon = TaxonCacheParser.parseLine(labeledCSVParser);
                return new Fun.Tuple2<String, Map<String, String>>(valueOrNoMatch(taxon.getExternalId()), TaxonUtil.taxonToMap(taxon));
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
