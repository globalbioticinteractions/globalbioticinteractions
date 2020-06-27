package org.globalbioticinteractions.dataset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class DatasetFinderUtil {

    public static URI getLocalDatasetURIRoot(File localDatasetURI) throws IOException {
        try (ZipFile zipFile = new ZipFile(localDatasetURI)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            String archiveRoot = null;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    archiveRoot = entry.getName();
                    break;
                }
            }

            return URI.create("jar:" + localDatasetURI.toURI() + "!/" + archiveRoot);
        }
    }

    public static String getLocalDatasetURIRoot(InputStream zipStream) {
        String archiveRoot = "";
        try (ZipInputStream is = new ZipInputStream(zipStream)) {
            ZipEntry entry = is.getNextEntry();
            if (entry != null && entry.isDirectory()) {
                archiveRoot = entry.getName();
            }
        } catch (IOException ex) {
            //
        }
        return archiveRoot;
    }

}
