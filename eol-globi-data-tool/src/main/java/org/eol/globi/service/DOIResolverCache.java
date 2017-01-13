package org.eol.globi.service;

import com.Ostermiller.util.CSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.util.CSVUtil;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Fun;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DOIResolverCache extends CacheService implements DOIResolver {
    private static final Log LOG = LogFactory.getLog(DOIResolverCache.class);

    private final String doiCacheResource;
    private Map<String, String> doiCitationMap = null;

    public DOIResolverCache() {
        this("/tsv/citations.tsv.gz");
    }

    public DOIResolverCache(String doiCacheResource) {
        this.doiCacheResource = doiCacheResource;
    }

    @Override
    public Map<String, String> resolveDoiFor(Collection<String> references) throws IOException {
        Map<String, String> results = new HashMap<>();
        for (String reference : references) {
            String doi = getDoiCitationMap().get(reference);
            if (StringUtils.isNotBlank(doi)) {
                results.put(reference, doi);
            }
        }
        return results;
    }

    public Map<String, String> getDoiCitationMap() {
        if (doiCitationMap == null) {
            try {
                BufferedReader bufferedReader = createBufferedReader(doiCacheResource);
                init(bufferedReader);
            } catch (PropertyEnricherException | IOException e) {
                LOG.warn("failed to initialize doi cache using [" + doiCacheResource + "], cache disabled", e);
                doiCitationMap = new HashMap<>();
            }
        }
        return doiCitationMap;
    }

    @Override
    public String resolveDoiFor(final String reference) throws IOException {
        return getDoiCitationMap().get(reference);
    }

    void init(final Reader reader) throws PropertyEnricherException, IOException {
        DB db = initDb("doiCache");
        StopWatch watch = new StopWatch();
        watch.start();
        final CSVParser parser = CSVUtil.createTSVParser(reader);

        LOG.info("doi cache building...");
        doiCitationMap = db
                .createTreeMap("doiCache")
                .pumpPresort(100000)
                .pumpIgnoreDuplicates()
                .pumpSource(new Iterator<Fun.Tuple2<String, String>>() {
                    private String[] line;

                    @Override
                    public boolean hasNext() {
                        try {
                            do {
                                line = parser.getLine();
                            }
                            while (line != null
                                    && line.length > 1
                                    && !StringUtils.isNoneBlank(line[0], line[1]));

                            return line != null
                                    && line.length > 1
                                    && StringUtils.isNoneBlank(line[0], line[1]);
                        } catch (IOException e) {
                            LOG.error("problem reading", e);
                            return false;
                        }
                    }

                    @Override
                    public Fun.Tuple2<String, String> next() {
                        return new Fun.Tuple2<>(line[0], line[1]);
                    }
                })
                .keySerializer(BTreeKeySerializer.STRING)
                .make();
        watch.stop();
        LOG.info("doi cache built in [" + watch.getTime() / 1000 + "] s.");
    }

}
