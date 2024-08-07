package org.globalbioticinteractions.dataset;

import jdk.nashorn.internal.ir.ObjectNode;
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
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;

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
    public Iterable<String> findNamespaces() throws DatasetRegistryException {
        List<String> namespaces = new ArrayList<>();
        findNamespaces(namespaces::add);
        return namespaces;
    }

    @Override
    public void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException {
        File directory = new File(cacheDir);
        Collection<String> namespaces = Collections.emptyList();
        if (directory.exists() && directory.isDirectory()) {
            namespaces = collectNamespaces(directory, namespaceConsumer);
        } else {
            LOG.warn("Directory [" + cacheDir + "] does not exist.");
        }
    }


    private Collection<String> collectNamespaces(File directory, Consumer<String> namespaceConsumer) throws DatasetRegistryException {

        try {
            Collection<String> namespaces = new TreeSet<>();

            Files.walkFileTree(
                    directory.toPath(),
                    EnumSet.of(FOLLOW_LINKS),
                    3,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file,
                                                         BasicFileAttributes attrs) {
                            FileVisitResult result = CONTINUE;
                            if (file.endsWith(ProvenanceLog.PROVENANCE_LOG_FILENAME)) {
                                try {
                                    addNamespace(namespaces, file.toFile(), namespaceConsumer);
                                    result = SKIP_SIBLINGS;
                                } catch (DatasetRegistryException e) {
                                    LOG.warn("failed to process [" + file.toFile().getAbsolutePath() + "]");
                                }
                            }
                            return result;
                        }
                    });
            return namespaces;
        } catch (IOException e) {
            throw new DatasetRegistryException("failed to traverse directory tree starting at [" + directory.getAbsolutePath() + "]");
        }


    }

    private void addNamespace(Collection<String> namespaces, File accessFile, Consumer<String> consumer) throws DatasetRegistryException {
        try {
            LineReaderFactory lineReaderFactory = new ReverseLineReaderFactoryImpl();
            final ProvenanceLog.ProvenanceEntryListener lineListener = new ProvenanceLog.ProvenanceEntryListener() {
                AtomicBoolean foundNamespace = new AtomicBoolean(false);

                @Override
                public void onValues(String[] values) {
                    if (values.length >= 5
                            && StringUtils.equals(values[4], CacheUtil.MIME_TYPE_GLOBI)) {
                        namespaces.add(values[0]);
                        consumer.accept(values[0]);
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
            public Iterable<String> findNamespaces() throws DatasetRegistryException {
                return Collections.singletonList(namespace);
            }

            @Override
            public void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException {
                for (String namespace : findNamespaces()) {
                    namespaceConsumer.accept(namespace);
                }
            }

            @Override
            public Dataset datasetFor(String s) throws DatasetRegistryException {
                Dataset dataset = new DatasetWithResourceMapping(namespace, sourceURI, resourceService);
                try {
                    return new DatasetWithCache(dataset,
                            cacheFactory.cacheFor(dataset));
                } catch (IOException e) {
                    throw new DatasetRegistryException(e);
                }
            }
        }).datasetFor(namespace);

        if (dataset == null) {
            throw new DatasetRegistryException("failed to retrieve/cache dataset in namespace [" + namespace + "]");
        }

        return dataset;
    }

}
