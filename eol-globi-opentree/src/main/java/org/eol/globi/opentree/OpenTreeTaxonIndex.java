package org.eol.globi.opentree;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class OpenTreeTaxonIndex {
    private static final Log LOG = LogFactory.getLog(OpenTreeTaxonIndex.class);
    private final URL openTreeTaxonomyUrl;

    private HTreeMap<String, Long> map = null;

    public OpenTreeTaxonIndex(URL openTreeTaxonomyUrl) {
        this.openTreeTaxonomyUrl = openTreeTaxonomyUrl;
    }

    private static final Set<String> prefix = new HashSet<String>();

    public Long findOpenTreeTaxonIdFor(String externalId) {
        if (map == null) {
            build();
        }
        return map.get(externalId);
    }


    protected void build() {
        final StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        LOG.info("OpenTreeTaxonIndex building...");
        map = buildMap();
        stopwatch.stop();
        if (map != null) {
            LOG.info("OpenTreeTaxonIndex built in " + String.format("%.1f", (float) stopwatch.getTime() / (1000.0)) + "s.");
        }  else {
            LOG.info("OpenTreeTaxonIndex failed to build.");
        }
    }

    public void destroy() {
        if (map != null) {
            map.close();
        }
    }

    protected HTreeMap<String, Long> buildMap() {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final HTreeMap<String, Long> idLookup = db
                .createHashMap("ottIdMap")
                .make();

        OpenTreeListener taxonListener = new OpenTreeListener() {
            @Override
            public void taxonSameAs(String ottId, String nonOttId) {
                long value = Long.parseLong(ottId);
                idLookup.put(nonOttId, value);
                String[] split = nonOttId.split(":");
                if (split.length > 1) {
                    prefix.add(split[0] + ":");
                }
            }
        };

        try {
            if (openTreeTaxonomyUrl == null) {
                LOG.error("failed to import open tree taxonomy: no taxonomy input stream");
            } else {
                OpenTreeUtil.readTaxonomy(taxonListener, openTreeTaxonomyUrl.openStream());
            }
        } catch (IOException e) {
            LOG.error("failed to build open tree taxon map map", e);
        }

        return idLookup;
    }

    public Collection<String> findUniqueExternalIdPrefixes() {
        if (prefix.isEmpty()) {
            build();
        }
        return prefix;
    }
}
