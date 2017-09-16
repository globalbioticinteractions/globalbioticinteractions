package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CacheLog {

    public final static String ACCESS_LOG_FILENAME = "access.tsv";

    static void appendCacheLog(String namespace, URI resourceURI, File cacheDir, URI localResourceCacheURI) throws IOException {
        Date accessedAt = new Date();
        String sha256 = new File(localResourceCacheURI).getName();
        CachedURI meta = new CachedURI(namespace, resourceURI, localResourceCacheURI, sha256, accessedAt);
        appendAccessLog(meta, getAccessFile(cacheDir));
    }

    public static void appendAccessLog(CachedURI meta, File accessLog) throws IOException {
        List<String> accessLogEntry = compileLogEntries(meta);
        String prefix = accessLog.exists() ? "\n" : "";
        String accessLogLine = StringUtils.join(accessLogEntry, '\t');
        FileUtils.writeStringToFile(accessLog, prefix + accessLogLine, true);
    }

    static List<String> compileLogEntries(CachedURI meta) {
        return Arrays.asList(meta.getNamespace()
                , meta.getSourceURI().toString()
                , meta.getSha256() == null ? "" : meta.getSha256()
                , ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(meta.getAccessedAt().getTime())
               , meta.getType());
    }

    public static File getAccessFile(String namespace, String cacheDir) {
        return getAccessFile(new File(cacheDir + "/" + namespace));
    }
    public static File getAccessFile(File dir) {
        return new File(dir, ACCESS_LOG_FILENAME);
    }
}
