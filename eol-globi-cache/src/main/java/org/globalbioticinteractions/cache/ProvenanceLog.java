package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.DatasetRegistryException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProvenanceLog {

    public final static String PROVENANCE_LOG_FILENAME = "access.tsv";

    public static void appendProvenanceLog(File cacheDir, ContentProvenance contentProvenance) throws IOException {
        if (needsCaching(contentProvenance, cacheDir)) {
            appendProvenanceLog(contentProvenance, cacheDir);
        }
    }

    static boolean needsCaching(ContentProvenance contentProvenance, File cacheDir) {
        boolean isCacheDir = ResourceUtil.isFileURI(contentProvenance.getSourceURI())
                && StringUtils.startsWith(new File(contentProvenance.getSourceURI()).getAbsolutePath(), cacheDir.getAbsolutePath());
        return !CacheLocalReadonly.isJarResource(contentProvenance.getLocalURI()) && !isCacheDir;
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

    static List<String> compileLogEntries(ContentProvenance contentProvenance) {
        List<String> logEntries;
        if (CacheLocalReadonly.isJarResource(contentProvenance.getLocalURI())) {
            logEntries = Collections.emptyList();
        } else {
            logEntries = Arrays.asList(contentProvenance.getNamespace()
                    , contentProvenance.getSourceURI().toString()
                    , contentProvenance.getSha256() == null ? "" : contentProvenance.getSha256()
                    , contentProvenance.getAccessedAt()
                    , contentProvenance.getType());
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

    public static void parseProvenanceStream(InputStream is, ProvenanceEntryListener listener) throws DatasetRegistryException {
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            BufferedReader bufferedReader = IOUtils.toBufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                listener.onValues(CSVTSVUtil.splitTSV(line));
            }
        } catch (IOException e) {
            throw new DatasetRegistryException("failed to read ", e);
        }
    }

    public interface ProvenanceEntryListener {
        void onValues(String[] values);
    }
}
