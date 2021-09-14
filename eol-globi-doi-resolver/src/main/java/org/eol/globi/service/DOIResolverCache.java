package org.eol.globi.service;

import com.Ostermiller.util.CSVParse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.mapdb.DB;
import org.mapdb.Fun;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DOIResolverCache extends CacheService implements DOIResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DOIResolverCache.class);

    private final String doiCacheResource;
    private Map<String, DOI> doiCitationMap = null;

    public DOIResolverCache() {
        this("/tsv/citations.tsv.gz");
    }

    public DOIResolverCache(String doiCacheResource) {
        this.doiCacheResource = doiCacheResource;
    }

    @Override
    public Map<String, DOI> resolveDoiFor(Collection<String> references) throws IOException {
        Map<String, DOI> results = new TreeMap<>();
        for (String reference : references) {
            DOI doi = getDoiCitationMap().get(reference);
            if (doi != null) {
                results.put(reference, doi);
            }
        }
        return results;
    }

    public Map<String, DOI> getDoiCitationMap() {
        if (doiCitationMap == null) {
            try {
                LOG.info("loading doi cache at [" + doiCacheResource + "]");
                BufferedReader bufferedReader = CacheServiceUtil.createBufferedReader(doiCacheResource);
                init(bufferedReader);
            } catch (PropertyEnricherException | IOException e) {
                LOG.warn("failed to initialize doi cache using [" + doiCacheResource + "], cache disabled", e);
                doiCitationMap = new TreeMap<>();
            }
        }
        return doiCitationMap;
    }

    @Override
    public DOI resolveDoiFor(final String reference) {
        return getDoiCitationMap().get(reference);
    }

    public void init(final Reader reader) throws PropertyEnricherException, IOException {
        DB db = initDb("doiCache");
        StopWatch watch = new StopWatch();
        watch.start();
        final CSVParse parser = CSVTSVUtil.createTSVParser(reader);
        if (db.exists("doiCache")) {
            LOG.info("reusing existing doi cache...");
        } else {
            LOG.info("doi cache building...");
            doiCitationMap = db
                    .createTreeMap("doiCache")
                    .pumpPresort(300000)
                    .pumpIgnoreDuplicates()
                    .pumpSource(new Iterator<Fun.Tuple2<String, DOI>>() {
                        private String[] line = null;
                        final AtomicBoolean nextLineParsed = new AtomicBoolean(false);

                        String getCitation(String[] line) {
                            return line != null && line.length > 1 ? line[1] : null;
                        }

                        DOI getDOI(String[] line) {
                            String doiString = line[0];
                            try {
                                return StringUtils.isBlank(doiString) ? null : DOI.create(doiString);
                            } catch (MalformedDOIException e) {
                                LOG.warn("skipping malformed doi [" + doiString + "]", e);
                                return null;
                            }
                        }

                        @Override
                        public boolean hasNext() {
                            try {
                                while (!nextLineParsed.get()) {
                                    line = parser.getLine();
                                    if (line == null) {
                                        break;
                                    }
                                    nextLineParsed.set(getDOI(line) != null
                                            && StringUtils.isNotBlank(getCitation(line)));
                                }
                                return line != null && nextLineParsed.get();
                            } catch (IOException e) {
                                LOG.error("problem reading", e);
                                return false;
                            }
                        }

                        @Override
                        public Fun.Tuple2<String, DOI> next() {
                            String citationString = StringUtils.defaultString(getCitation(line), "");
                            DOI doi = getDOI(line);
                            nextLineParsed.set(false);
                            return new Fun.Tuple2<>(citationString, doi);
                        }
                    })
                    .make();
            db.commit();
            watch.stop();
            LOG.info("doi cache built in [" + watch.getTime(TimeUnit.SECONDS) + "] s.");
        }
    }

}
