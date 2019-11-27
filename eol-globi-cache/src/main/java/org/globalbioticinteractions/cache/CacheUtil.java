package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.eol.globi.util.InputStreamFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public final class CacheUtil {

    public static final String MIME_TYPE_GLOBI = "application/globi";

    public static Cache cacheFor(String namespace, String cacheDir, InputStreamFactory inputStreamFactory) {
        Cache pullThroughCache = new CachePullThrough(namespace, cacheDir, inputStreamFactory);
        CacheLocalReadonly readOnlyCache = new CacheLocalReadonly(namespace, cacheDir, inStream -> inStream);
        return new CacheProxy(Arrays.asList(readOnlyCache, pullThroughCache));
    }

    public static File getCacheDirForNamespace(String cachePath, String namespace) throws IOException {
        File cacheDir = new File(cachePath);
        FileUtils.forceMkdir(cacheDir);
        File directory = new File(cacheDir, namespace);
        FileUtils.forceMkdir(directory);
        return directory;
    }
}
