package org.eol.globi.service;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;

import static org.eol.globi.service.CacheServiceUtil.createCacheDir;

public class CacheService {

    private File cacheDir = new File("./target/term-cache");

    public DB initDb(String cacheName) throws PropertyEnricherException {
        File mapdbCacheDir = getMapDBDir();
        if (!mapdbCacheDir.exists()) {
            createCacheDir(getMapDBDir());
        }
        File mapDBFile = new File(mapdbCacheDir, cacheName);

        DBMaker dbMaker = DBMaker
                .newFileDB(mapDBFile)
                .mmapFileEnableIfSupported()
                .transactionDisable()
                .closeOnJvmShutdown();
        return dbMaker
                .make();
    }

    private File getMapDBDir() {
        return new File(getCacheDir(), "mapdb");
    }

    public void setCacheDir(File cacheFilename) {
        this.cacheDir = cacheFilename;
    }

    public File getCacheDir() {
        return this.cacheDir;
    }

}
