package org.eol.globi.service;

import org.apache.commons.io.FileUtils;
import org.eol.globi.util.ResourceUtil;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class CacheService {

    private File cacheDir = new File("./mapdb/");

    public static BufferedReader createBufferedReader(String taxonResourceUrl) throws IOException {
        return new BufferedReader(new InputStreamReader(ResourceUtil.asInputStream(taxonResourceUrl)));
    }

    public DB initDb(String cacheName) throws PropertyEnricherException {
        createCacheDir(cacheDir);
        return DBMaker
                .newFileDB(new File(cacheDir, cacheName))
                .deleteFilesAfterClose()
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();
    }

    public static void createCacheDir(File cacheDir) throws PropertyEnricherException {
        FileUtils.deleteQuietly(cacheDir);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }
    }

    public void setCacheDir(File cacheFilename) {
        this.cacheDir = cacheFilename;
    }

}
