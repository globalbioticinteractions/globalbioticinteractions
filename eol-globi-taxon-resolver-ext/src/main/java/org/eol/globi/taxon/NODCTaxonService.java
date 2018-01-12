package org.eol.globi.taxon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ResourceUtil;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class NODCTaxonService implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(NODCTaxonService.class);

    private File cacheDir = new File("./nodc.mapdb");
    private BTreeMap<String, String> nodc2itis = null;
    private PropertyEnricher itisService = new ITISService();
    private String nodcResourceUrl = System.getProperty("nodc.url");

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        Map<String, String> enriched = new TreeMap<String, String>(properties);
        if (StringUtils.startsWith(externalId, TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER.getIdPrefix())) {
            if (needsInit()) {
                lazyInit();
            }
            final String tsn = nodc2itis.get(externalId);
            if (StringUtils.isNotBlank(tsn)) {
                enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, tsn);
                enriched = itisService.enrich(enriched);
            }
        }
        return enriched;
    }

    public boolean needsInit() {
        return nodc2itis == null;
    }

    public String getNodcResourceUrl() {
        return nodcResourceUrl;
    }

    private void lazyInit() throws PropertyEnricherException {
        String nodcFilename = getNodcResourceUrl();
        if (StringUtils.isBlank(nodcResourceUrl)) {
            throw new PropertyEnricherException("cannot initialize NODC enricher: failed to find NODC taxon file. Did you install the NODC taxonomy and set -DnodcFile=...?");
        }
        try {
            NODCTaxonParser parser = new NODCTaxonParser(new BufferedReader(new InputStreamReader(ResourceUtil.asInputStream(nodcFilename))));
            init(parser);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to read from NODC resource [" + nodcFilename + "]", e);
        }
    }

    protected void init(NODCTaxonParser parser) throws PropertyEnricherException {
        CacheService.createCacheDir(cacheDir);

        LOG.info("NODC taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();

        DB db = DBMaker
                .newFileDB(new File(cacheDir, "nodcLookup"))
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        nodc2itis = db
                .createTreeMap("nodc2itis")
                .pumpSource(parser)
                .pumpPresort(100000)
                .pumpIgnoreDuplicates()
                .keySerializer(BTreeKeySerializer.STRING)
                .make();

        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), nodc2itis.size(), LOG);
        LOG.info("NODC taxonomy imported.");
    }

    @Override
    public void shutdown() {
        if (nodc2itis != null) {
            TaxonCacheService.close(nodc2itis.getEngine());
            nodc2itis = null;
        }
        FileUtils.deleteQuietly(cacheDir);
    }
}
