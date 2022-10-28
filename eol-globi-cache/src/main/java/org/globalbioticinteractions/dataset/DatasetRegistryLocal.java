package org.globalbioticinteractions.dataset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.cache.CacheFactory;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.LineReaderFactory;
import org.globalbioticinteractions.cache.ProvenanceLog;
import org.globalbioticinteractions.cache.ReverseLineReaderFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DatasetRegistryLocal implements DatasetRegistry {
    private final static Logger LOG = LoggerFactory.getLogger(DatasetRegistryLocal.class);
    private final String cacheDir;
    private final CacheFactory cacheFactory;
    private ResourceService resourceService;

    public DatasetRegistryLocal(String cacheDir,
                                CacheFactory cacheFactory,
                                ResourceService resourceService) {
        this.cacheDir = cacheDir;
        this.cacheFactory = cacheFactory;
        this.resourceService = resourceService;
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetRegistryException {
        File directory = new File(cacheDir);
        Collection<String> namespaces = Collections.emptyList();
        if (directory.exists() && directory.isDirectory()) {
            namespaces = collectNamespaces(directory);
        } else {
            LOG.warn("Directory [" + cacheDir + "] does not exist.");
        }
        return namespaces;
    }

    private Collection<String> collectNamespaces(File directory) throws DatasetRegistryException {
        Collection<File> accessFiles = FileUtils.listFiles(directory, new FileFileFilter() {
            @Override
            public boolean accept(File file) {
                return ProvenanceLog.PROVENANCE_LOG_FILENAME.endsWith(file.getName());
            }
        }, TrueFileFilter.INSTANCE);

        Collection<String> namespaces = new TreeSet<>();


        for (File accessFile : accessFiles) {
            try {
                LineReaderFactory lineReaderFactory = new ReverseLineReaderFactoryImpl();
                final ProvenanceLog.ProvenanceEntryListener lineListener = new ProvenanceLog.ProvenanceEntryListener() {
                    AtomicBoolean foundNamespace = new AtomicBoolean(false);
                    @Override
                    public void onValues(String[] values) {
                        if (values.length >= 5
                                && StringUtils.equals(values[4], CacheUtil.MIME_TYPE_GLOBI)) {
                            namespaces.add(values[0]);
                            foundNamespace.set(true);
                        }
                    }

                    @Override
                    public boolean shouldContinue() {
                        return !foundNamespace.get();
                    }
                };

                ProvenanceLog.parseProvenanceLogFile(accessFile, lineListener, lineReaderFactory);
            } catch (DatasetRegistryException e) {
                throw new DatasetRegistryException("failed to access [" + accessFile.getAbsolutePath() + "]", e);
            }
        }
        return namespaces;
    }


    private URI findLastCachedDatasetURI(String namespace) throws DatasetRegistryException {
        AtomicReference<URI> sourceURI = new AtomicReference<>();
        File accessFile = ProvenanceLog.findProvenanceLogFile(namespace, cacheDir);
        if (accessFile.exists()) {
            LineReaderFactory lineReaderFactory = new ReverseLineReaderFactoryImpl();
            final ProvenanceLog.ProvenanceEntryListener lineListener = new ProvenanceLog.ProvenanceEntryListener() {
                @Override
                public void onValues(String[] values) {
                    if (values.length > 4
                            && StringUtils.equalsIgnoreCase(StringUtils.trim(values[0]), namespace)
                            && StringUtils.equals(StringUtils.trim(values[4]), CacheUtil.MIME_TYPE_GLOBI)) {
                        sourceURI.set(URI.create(values[1]));
                    }
                }

                @Override
                public boolean shouldContinue() {
                    return sourceURI.get() == null;
                }
            };

            ProvenanceLog.parseProvenanceLogFile(accessFile, lineListener, lineReaderFactory);
        }
        return sourceURI.get();
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        Dataset dataset;

        final URI sourceURI = findLastCachedDatasetURI(namespace);
        dataset = sourceURI == null ? null : new DatasetFactory(new DatasetRegistry() {
            @Override
            public Collection<String> findNamespaces() throws DatasetRegistryException {
                return Collections.singletonList(namespace);
            }

            @Override
            public Dataset datasetFor(String s) throws DatasetRegistryException {
                Dataset dataset = new DatasetWithResourceMapping(namespace, sourceURI, resourceService);
                return new DatasetWithCache(dataset,
                        cacheFactory.cacheFor(dataset));
            }
        }).datasetFor(namespace);

        if (dataset == null) {
            throw new DatasetRegistryException("failed to retrieve/cache dataset in namespace [" + namespace + "]");
        }

        return dataset;
    }

}
