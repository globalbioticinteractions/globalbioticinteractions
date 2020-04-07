package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProvenanceLog {

    public final static String PROVENANCE_LOG_FILENAME = "access.tsv";

    public static void appendProvenanceLog(File cacheDir, ContentProvenance contentProvenance) throws IOException {
        appendProvenanceLog(contentProvenance, cacheDir);
    }

    private static void appendProvenanceLog(ContentProvenance contentProvenance, File cacheDir) throws IOException {
        List<String> accessLogEntry = compileLogEntries(contentProvenance);
        File accessLog = findProvenanceLogFile(contentProvenance.getNamespace(), cacheDir.getAbsolutePath());
        String prefix = accessLog.exists() ? "\n" : "";
        String accessLogLine = StringUtils.join(accessLogEntry, '\t');
        try {
            FileUtils.writeStringToFile(accessLog, prefix + accessLogLine, StandardCharsets.UTF_8, true);
        } catch (IOException ex) {
            throw new IOException("failed to write to [" + accessLog.getAbsolutePath() + "]", ex);
        }
    }

    static List<String> compileLogEntries(ContentProvenance meta) {
        List<String> logEntries;
        if (CacheLocalReadonly.isJarResource(meta.getLocalURI())) {
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

    public static File findProvenanceLogFile(String namespace, String cacheDir) throws IOException {
        File cacheDirForNamespace = CacheUtil.findCacheDirForNamespace(cacheDir, namespace);
        return getProvenanceLogFile(cacheDirForNamespace);
    }

    private static File getProvenanceLogFile(File dir) {
        return new File(dir, PROVENANCE_LOG_FILENAME);
    }
}
