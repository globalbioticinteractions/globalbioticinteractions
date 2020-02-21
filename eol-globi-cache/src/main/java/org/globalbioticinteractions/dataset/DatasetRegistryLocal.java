package org.globalbioticinteractions.dataset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.cache.CacheFactory;
import org.globalbioticinteractions.cache.ProvenanceLog;
import org.globalbioticinteractions.cache.CacheUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public class DatasetRegistryLocal implements DatasetRegistry {
    private final static Log LOG = LogFactory.getLog(DatasetRegistryLocal.class);
    private final String cacheDir;
    private final CacheFactory cacheFactory;
    private final InputStreamFactory inputStreamFactory;

    public DatasetRegistryLocal(String cacheDir, CacheFactory cacheFactory) {
        this(cacheDir, cacheFactory, inStream -> inStream);
    }

    public DatasetRegistryLocal(String cacheDir, CacheFactory cacheFactory, InputStreamFactory factory) {
        this.cacheDir = cacheDir;
        this.cacheFactory = cacheFactory;
        this.inputStreamFactory = factory;
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetFinderException {
        File directory = new File(cacheDir);
        Collection<String> namespaces = Collections.emptyList();
        if (directory.exists() && directory.isDirectory()) {
            namespaces = collectNamespaces(directory);
        } else {
            LOG.warn("Directory [" + cacheDir + "] does not exist.");
        }
        return namespaces;
    }

    private interface AccessFileLineListener {
        void onValues(String[] values);
    }

    private Collection<String> collectNamespaces(File directory) throws DatasetFinderException {
        Collection<File> accessFiles = FileUtils.listFiles(directory, new FileFileFilter() {
            @Override
            public boolean accept(File file) {
                return ProvenanceLog.PROVENANCE_LOG_FILENAME.endsWith(file.getName());
            }
        }, TrueFileFilter.INSTANCE);

        Collection<String> namespaces = new TreeSet<>();
        AccessFileLineListener lineListener = values -> {
            if (values.length > 0) {
                namespaces.add(values[0]);
            }
        };

        for (File accessFile : accessFiles) {
            scanAccessFile(accessFile, lineListener);
        }
        return namespaces;
    }

    private void scanAccessFile(File accessFile, AccessFileLineListener accessLine) throws DatasetFinderException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(accessFile), StandardCharsets.UTF_8)) {
            BufferedReader bufferedReader = IOUtils.toBufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                accessLine.onValues(CSVTSVUtil.splitTSV(line));
            }
        } catch (IOException e) {
            throw new DatasetFinderException("failed to read ", e);
        }
    }


    private URI findLastCachedDatasetURI(String namespace) throws DatasetFinderException {
        AtomicReference<URI> sourceURI = new AtomicReference<>();
        AccessFileLineListener accessFileLineListener = values -> {
            if (values.length > 4
                    && StringUtils.equalsIgnoreCase(StringUtils.trim(values[0]), namespace)
                    && StringUtils.equals(StringUtils.trim(values[4]), CacheUtil.MIME_TYPE_GLOBI)) {
                sourceURI.set(URI.create(values[1]));
            }

        };
        File accessFile;
        try {
            accessFile = ProvenanceLog.findProvenanceLogFile(namespace, cacheDir);
        } catch (IOException e) {
            throw new DatasetFinderException("issue accessing provenance log", e);
        }
        if (accessFile.exists()) {
            scanAccessFile(accessFile, accessFileLineListener);
        }
        return sourceURI.get();
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        Dataset dataset;

        final URI sourceURI = findLastCachedDatasetURI(namespace);
        dataset = sourceURI == null ? null : new DatasetFactory(new DatasetRegistry() {
            @Override
            public Collection<String> findNamespaces() throws DatasetFinderException {
                return Collections.singletonList(namespace);
            }

            @Override
            public Dataset datasetFor(String s) throws DatasetFinderException {
                Dataset dataset = new DatasetImpl(namespace, sourceURI, getInputStreamFactory());
                return new DatasetWithCache(dataset,
                        cacheFactory.cacheFor(dataset));
            }
        }).datasetFor(namespace);

        if (dataset == null) {
            throw new DatasetFinderException("failed to retrieve/cache dataset in namespace [" + namespace + "]");
        }

        return dataset;
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }


}
