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

    private File cacheDir = new File("./target/term-cache");

    private boolean temporary = true;

    public static BufferedReader createBufferedReader(String taxonResourceUrl) throws IOException {
        return new BufferedReader(new InputStreamReader(ResourceUtil.asInputStream(taxonResourceUrl)));
    }

    public DB initDb(String cacheName) throws PropertyEnricherException {
        File mapdbCacheDir = new File(getCacheDir(), "mapdb");
        createCacheDir(mapdbCacheDir, isTemporary());
        DBMaker dbMaker = DBMaker
                .newFileDB(new File(mapdbCacheDir, cacheName))
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .transactionDisable();
        if (isTemporary()) {
            dbMaker.deleteFilesAfterClose();
        }
        return dbMaker
                .make();
    }

    public static void createCacheDir(File cacheDir) throws PropertyEnricherException {
        createCacheDir(cacheDir, true);
    }

    protected static void createCacheDir(File cacheDir, boolean temporary) throws PropertyEnricherException {
        if (temporary) {
            FileUtils.deleteQuietly(cacheDir);
        }
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }
    }

    public void setCacheDir(File cacheFilename) {
        this.cacheDir = cacheFilename;
    }

    public File getCacheDir() {
        return this.cacheDir;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }
}
