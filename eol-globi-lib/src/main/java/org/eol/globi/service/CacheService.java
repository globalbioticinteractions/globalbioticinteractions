package org.eol.globi.service;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;

public class CacheService {

    private File cacheDir = new File("./target/term-cache");

    public DB initDb(String cacheName) throws PropertyEnricherException {
        File mapdbCacheDir = new File(getCacheDir(), "mapdb");
        CacheServiceUtil.createCacheDir(mapdbCacheDir);
        DBMaker dbMaker = DBMaker
                .newFileDB(new File(mapdbCacheDir, cacheName))
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .transactionDisable()
                .deleteFilesAfterClose();
        return dbMaker
                .make();
    }

    public void setCacheDir(File cacheFilename) {
        this.cacheDir = cacheFilename;
    }

    public File getCacheDir() {
        return this.cacheDir;
    }

}
