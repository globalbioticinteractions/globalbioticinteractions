package org.globalbioticinteractions.dataset;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DatasetFinderUtil {

    static URI getLocalDatasetURIRoot(File localDatasetURI) throws IOException {
        Enumeration<? extends ZipEntry> entries = new ZipFile(localDatasetURI).entries();

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
