package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class DatasetFinderUtil {

    public static URI getLocalDatasetURIRoot(File localDatasetURI) throws IOException {
        try (ZipFile zipFile = new ZipFile(localDatasetURI)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            PrefixCalculator commonPrefixCalculator = new PrefixCalculator();

            while (entries.hasMoreElements()) {
                commonPrefixCalculator.accept(entries.nextElement());
            }

            return URI.create("jar:" + localDatasetURI.toURI() + "!/" + commonPrefixCalculator.getCommonPrefix());
        }
    }

    public static String getLocalDatasetURIRoot(InputStream zipStream) {
        String archiveRoot = "";
        PrefixCalculator prefixCalculator = new PrefixCalculator();
        try (ZipInputStream is = new ZipInputStream(zipStream)) {
            ZipEntry entry = null;
            while ((entry = is.getNextEntry()) != null) {
                prefixCalculator.accept(entry);
            }
        } catch (IOException ex) {
            //
        }
        return prefixCalculator.getCommonPrefix();
    }

    private static class PrefixCalculator implements Consumer<ZipEntry> {

        private String commonPrefix = "";

        @Override
        public void accept(ZipEntry entry) {
            commonPrefix = StringUtils.getCommonPrefix(
                    StringUtils.defaultIfBlank(commonPrefix, entry.getName()),
                    entry.getName()
            );

        }

        public String getCommonPrefix() {
            int lastSlash = StringUtils.lastIndexOf(commonPrefix, "/");
            return (lastSlash > 0 ? commonPrefix.substring(0, lastSlash + 1) : "");
        }
    }
}
