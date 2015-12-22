package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVUtil;
import org.eol.globi.util.ResourceUtil;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Engine;
import org.mapdb.Fun;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class TaxonCacheService implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonCacheService.class);

    private BTreeMap<String, Map<String, String>> resolvedIdToTaxonMap = null;
    private BTreeMap<String, String> providedToResolvedMap = null;
    private String taxonCacheResource;
    private final String taxonMapResource;

    private File cacheDir = new File("./mapdb/");

    public TaxonCacheService(String taxonCacheResource, String taxonMapResource) {
        this.taxonCacheResource = taxonCacheResource;
        this.taxonMapResource = taxonMapResource;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        lazyInit();
        Map<String, String> enriched = getTaxon(getExternalId(properties));
        if (enriched == null) {
            enriched = getTaxon(getName(properties));
        }
        return enriched == null ? Collections.unmodifiableMap(properties) : enriched;
    }

    public Map<String, String> getTaxon(String value) {
        Map<String, String> enriched = null;
        if (isNonEmptyValue(value)) {
            final String key = providedToResolvedMap.get(value);
            if (isNonEmptyValue(key)) {
                enriched = resolvedIdToTaxonMap.get(key);
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
        if (resolvedIdToTaxonMap == null || providedToResolvedMap == null) {
            init();
        }
    }

    public void init() throws PropertyEnricherException {
        LOG.info("taxon cache initializing...");
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }
        DB db = DBMaker
                .newFileDB(new File(cacheDir, "taxonCache"))
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        StopWatch watch = new StopWatch();
        watch.start();
        try {
            resolvedIdToTaxonMap = db
                    .createTreeMap("taxonCacheById")
                    .pumpPresort(100000)
                    .pumpIgnoreDuplicates()
                    .pumpSource(createTaxonCacheSource(taxonCacheResource))
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
            providedToResolvedMap = db
                    .createTreeMap("taxonMappingById")
                    .pumpSource(createTaxonMappingSource(taxonMapResource))
                    .pumpPresort(100000)
                    .pumpIgnoreDuplicates()
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to build taxon cache map", e);
        }
        watch.stop();
        logCacheLoadStats(watch.getTime(), providedToResolvedMap.size());

        LOG.info("taxon cache initialized.");
    }

    public Iterator<Fun.Tuple2<String, String>> createTaxonMappingSource(final String resource) throws IOException {
        return new Iterator<Fun.Tuple2<String, String>>() {
            private BufferedReader reader = createBufferedReader(resource);
            private final LabeledCSVParser labeledCSVParser = CSVUtil.createLabeledCSVParser(reader);
            private boolean processing = false;

            @Override
            public boolean hasNext() {
                try {
                    return processing || labeledCSVParser.getLine() != null;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public Fun.Tuple2<String, String> next() {
                final Taxon resolvedTaxon = TaxonMapParser.parseResolvedTaxon(labeledCSVParser);
                final Taxon providedTaxon = TaxonMapParser.parseProvidedTaxon(labeledCSVParser);
                final String key = processing ? providedTaxon.getExternalId() : providedTaxon.getName();
                processing = !processing;
                return new Fun.Tuple2<String, String>(valueOrNoMatch(key), valueOrNoMatch(resolvedTaxon.getExternalId()));
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    private String valueOrNoMatch(String value) {
        return isNonEmptyValue(value) ? value : PropertyAndValueDictionary.NO_MATCH;
    }

    public Iterator<Fun.Tuple2<String, Map<String, String>>> createTaxonCacheSource(final String resource) throws IOException {

        return new Iterator<Fun.Tuple2<String, Map<String, String>>>() {
            private BufferedReader reader = createBufferedReader(resource);
            private final LabeledCSVParser labeledCSVParser = CSVUtil.createLabeledCSVParser(reader);

            @Override
            public boolean hasNext() {
                try {
                    return labeledCSVParser.getLine() != null;
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

    public void logCacheLoadStats(long time, int numberOfItems) {
        final double avgRate = numberOfItems * 1000.0 / time;
        final double timeElapsedInSeconds = time / 1000.0;
        final String msg = String.format("cache with [%d]" + " items built in [%.1f] s or [%.1f] items/s.",
                numberOfItems,
                timeElapsedInSeconds,
                avgRate);
        LOG.info(msg);
    }

    public boolean isNonEmptyValue(String sourceValue) {
        return StringUtils.isNotBlank(sourceValue)
                && !StringUtils.equals(sourceValue, PropertyAndValueDictionary.NO_MATCH)
                && !StringUtils.equals(sourceValue, PropertyAndValueDictionary.NO_NAME);
    }

    public BufferedReader createBufferedReader(String taxonResourceUrl) throws IOException {
        return new BufferedReader(new InputStreamReader(ResourceUtil.asInputStream(taxonResourceUrl, TaxonCacheService.class)));
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
        FileUtils.deleteQuietly(cacheDir);
    }

    public void close(Engine engine) {
        if (!engine.isClosed()) {
            engine.close();
        }
    }

    public void setCacheDir(File cacheFilename) {
        this.cacheDir = cacheFilename;
    }

}
