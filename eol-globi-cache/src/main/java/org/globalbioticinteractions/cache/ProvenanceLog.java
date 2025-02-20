package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.dataset.DatasetRegistryException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ProvenanceLog {

    public final static String PROVENANCE_LOG_FILENAME = "access.tsv";

    public static void appendProvenanceLog(File cacheDir, ContentProvenance contentProvenance) throws IOException {
        if (needsCaching(contentProvenance, cacheDir)) {
            appendProvenanceLog(contentProvenance, new ContentPathDepth0(cacheDir, contentProvenance.getNamespace()));
        }
    }

    static boolean needsCaching(ContentProvenance contentProvenance, File cacheDir) {
        URI sourceURI = contentProvenance.getSourceURI();

        boolean isInCacheDir = CacheUtil.isInCacheDir(cacheDir, sourceURI);

        return !isInCacheDir
                && !CacheLocalReadonly.isJarResource(contentProvenance.getLocalURI());
    }

    private static void appendProvenanceLog(ContentProvenance contentProvenance, ContentPath contentPath) throws IOException {
        List<String> accessLogEntry = compileLogEntries(contentProvenance);
        File accessLog = getProvenanceLogFile(new ProvenancePathImpl(contentPath));
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

    public static File getProvenanceLogFile(ProvenancePath provenancePath) {
        return new File(provenancePath.get());
    }

    public static void parseProvenanceLogFile(File file, ProvenanceEntryListener listener, LineReaderFactory lineReaderFactory) throws DatasetRegistryException {
        try (LineReader lineReader = lineReaderFactory.createLineReader(file)) {
            String line;
            while (listener.shouldContinue() && (line = lineReader.readLine()) != null) {
                listener.onValues(CSVTSVUtil.splitTSV(line));
            }
        } catch (IOException e) {
            throw new DatasetRegistryException("failed to read ", e);
        }
    }

    public interface ProvenanceEntryListener {
        void onValues(String[] values);

        boolean shouldContinue();
    }

}
