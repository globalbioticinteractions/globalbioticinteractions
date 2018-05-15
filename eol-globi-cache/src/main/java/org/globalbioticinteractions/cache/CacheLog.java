package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CacheLog {

    public final static String ACCESS_LOG_FILENAME = "access.tsv";

    static void appendCacheLog(String namespace, URI resourceURI, File cacheDir, URI localResourceCacheURI) throws IOException {
        String accessedAt = ISODateTimeFormat.dateTime().withZoneUTC().print(new Date().getTime());
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
        List<String> logEntries;
        if (CacheLocalReadonly.isJarResource(meta.getCachedURI())) {
            logEntries = Collections.emptyList();
        } else {
            logEntries = Arrays.asList(meta.getNamespace()
                    , meta.getSourceURI().toString()
                    , meta.getSha256() == null ? "" : meta.getSha256()
                    , meta.getAccessedAt()
                    , meta.getType());
        }
        return logEntries;
    }

    public static File getAccessFile(String namespace, String cacheDir) {
        return getAccessFile(new File(cacheDir + "/" + namespace));
    }

    public static File getAccessFile(File dir) {
        return new File(dir, ACCESS_LOG_FILENAME);
    }
}
