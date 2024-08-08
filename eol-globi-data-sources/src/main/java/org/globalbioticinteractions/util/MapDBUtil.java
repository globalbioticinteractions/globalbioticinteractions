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
        return tmpDB(newTmpFileDB(tmpDir))
                .getTreeMap("temp");
    }

    private static DBMaker newTmpFileDB(File tmpDir) {
        try {
            File  db = File.createTempFile("mapdb-temp", "db", tmpDir);
            return DBMaker.newFileDB(db);
        } catch (IOException e) {
            throw new IOError(new IOException("failed to create tmpFile in [" + tmpDir.getAbsolutePath() + "]", e));
        }

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
