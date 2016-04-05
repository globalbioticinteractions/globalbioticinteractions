package org.eol.globi.taxon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class NODCTaxonService implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(NODCTaxonService.class);

    private String baseDir;
    private File cacheDir = new File("./nodc.mapdb");
    private BTreeMap<String, String> nodc2itis = null;

    public NODCTaxonService(String baseDir) {
        this.baseDir = baseDir;
    }

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
            }
        }
        return enriched;
    }

    public boolean needsInit() {
        return nodc2itis == null;
    }

    private void lazyInit() throws PropertyEnricherException {
        final String nodcResource = "/0050418/1.1/data/0-data/NODC_TaxonomicCode_V8_CD-ROM/TAXBRIEF.DAT";
        try {
            NODCTaxonParser parser = new NODCTaxonParser(new BufferedReader(new InputStreamReader(ResourceUtil.asInputStream(baseDir + nodcResource, null))));
            init(parser);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to read from NODC resource [" + nodcResource + "]", e);
        }
    }

    protected void init(NODCTaxonParser parser) throws PropertyEnricherException {
        TaxonCacheService.createCacheDir(cacheDir);

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
