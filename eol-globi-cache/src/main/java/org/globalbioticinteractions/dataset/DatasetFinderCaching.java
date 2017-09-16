package org.globalbioticinteractions.dataset;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetFinderException;
import org.globalbioticinteractions.cache.CacheLog;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.CachedURI;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

public class DatasetFinderCaching implements DatasetFinder {
    private final DatasetFinder finder;
    private final String cacheDir;

    public DatasetFinderCaching(DatasetFinder finder) {
        this(finder, "target/datasets");
    }
    public DatasetFinderCaching(DatasetFinder finder, String cacheDir) {
        this.finder = finder;
        this.cacheDir = cacheDir;
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetFinderException {
        return getFinder().findNamespaces();
    }

    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        Dataset dataset = getFinder().datasetFor(namespace);
        try {
            CachedURI meta = new CachedURI(namespace, dataset.getArchiveURI(), null, null, new Date());
            meta.setType(CacheUtil.MIME_TYPE_GLOBI);
            CacheLog.appendAccessLog(meta, CacheLog.getAccessFile(CacheUtil.getCacheDirForNamespace(getCacheDir(), namespace)));
        } catch (IOException e) {
            throw new DatasetFinderException("failed to record access", e);
        }
        return new DatasetWithCache(dataset, CacheUtil.cacheFor(namespace, getCacheDir()));

    }
    private String getCacheDir() {
        return cacheDir;
    }
    private DatasetFinder getFinder() {
        return finder;
    }




}
