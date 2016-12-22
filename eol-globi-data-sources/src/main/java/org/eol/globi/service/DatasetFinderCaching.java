package org.eol.globi.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DatasetFinderCaching implements DatasetFinder {

    private final DatasetFinder finder;

    public DatasetFinderCaching(DatasetFinder finder) {
        this.finder = finder;
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetFinderException {
        return this.finder.findNamespaces();
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        try {
            Dataset dataset = finder.datasetFor(namespace);
            return cache(dataset);
        } catch (IOException e) {
            throw new DatasetFinderException("failed to retrieve/cache dataset in namespace [" + namespace + "]",e);
        }
    }

    static Dataset cache(Dataset dataset) throws IOException {
        File cache = cache(dataset, "cache/datasets");
        return cacheArchive(dataset, cache);
    }

    static Dataset cacheArchive(Dataset dataset, File archiveCache) throws IOException {
        Enumeration<? extends ZipEntry> entries = new ZipFile(archiveCache).entries();

        String archiveRoot = null;
        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                archiveRoot = entry.getName();
                break;
            }
        }

        URI archiveCacheURI = URI.create("jar:" + archiveCache.toURI() + "!/" + archiveRoot);
        return new DatasetCached(dataset, archiveCacheURI);
    }

    static File cache(Dataset dataset, String pathname) throws IOException {
        File cacheDir = new File(pathname);
        FileUtils.forceMkdir(cacheDir);

        InputStream is = ResourceUtil.asInputStream(dataset.getArchiveURI(), null);
        File directory = new File(cacheDir, dataset.getNamespace());
        FileUtils.forceMkdir(directory);
        File archiveCache =  new File(directory, "archive.zip");
        FileUtils.copyInputStreamToFile(is, archiveCache);
        IOUtils.closeQuietly(is);
        return archiveCache;
    }
}
