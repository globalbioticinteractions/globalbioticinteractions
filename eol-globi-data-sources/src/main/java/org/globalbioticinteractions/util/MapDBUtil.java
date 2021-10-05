package org.globalbioticinteractions.util;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Set;

public class MapDBUtil {

    public static <K, V> BTreeMap<K, V> createBigMap(File tmpDir) {
        return getBigMap(newTmpFileDB(tmpDir));
    }

    private static DBMaker newTmpFileDB(File tmpDir) {
        try {
            return DBMaker.newFileDB(File.createTempFile("mapdb-temp", "db", tmpDir));
        } catch (IOException e) {
            throw new IOError(e);
        }

    }

    private static <K, V> BTreeMap<K, V> getBigMap(DBMaker dbMaker) {
        return tmpDB(dbMaker)
                .getTreeMap("temp");
    }

    public static <K, V> BTreeMap<K, V> createBigMap() {
        return getBigMap(DBMaker.newTempFileDB());
    }

    public static <T> Set<T> createBigSet(DB db) {
        return db.getHashSet("temp");
    }

    public static <T> Set<T> createBigSet(File tmpDir) {
        return createBigSet(newTmpFileDB(tmpDir));
    }

    public static DB tmpDB(File tmpDir) {
        return tmpDB(newTmpFileDB(tmpDir));
    }

    public static DB tmpDB() {
        return tmpDB(DBMaker.newTempFileDB());
    }

    private static DB tmpDB(DBMaker maker) {
        return maker
                .deleteFilesAfterClose()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();
    }


    private static <T> Set<T> createBigSet(DBMaker dbMaker) {
        return tmpDB(dbMaker)
                .getHashSet("temp");
    }
}
