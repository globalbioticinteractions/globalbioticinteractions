package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.cache.ContentPathFactoryDepth0;
import org.globalbioticinteractions.cache.ProvenancePathFactoryImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@CommandLine.Command(
        name = "compile",
        aliases = {"import"},
        description = "compile and import datasets into Neo4J"
)
public class CmdCompile extends CmdNeo4J {

    @CommandLine.Option(
            names = {"-f"},
            description = "file with names of dataset namespaces to compile: one line contains one namespace. " +
                    "E.g., globalbioticinteractions/template-dataset\nglobalbioticinteractions/ucsb-izc"
    )
    private String datasetNamespaceFile;

    @Override
    public void run() {
        ResourceService resourceService = new ResourceServiceLocal(new InputStreamFactoryNoop());

        List<String> namepaces = new ArrayList<>();
        if (StringUtils.isNotBlank(datasetNamespaceFile)) {
            try (InputStream retrieve = resourceService.retrieve(URI.create(datasetNamespaceFile))) {
                InputStreamReader inputStreamReader = new InputStreamReader(retrieve, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    namepaces.add(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        DatasetRegistry registry = DatasetRegistryUtil.getDatasetRegistry(
                getDatasetDir(),
                resourceService,
                new ContentPathFactoryDepth0(),
                new ProvenancePathFactoryImpl(),
                getProvenanceDir()
        );

        if (!namepaces.isEmpty()) {
            registry = new DatasetRegistryWithStaticNamespaces(namepaces, registry);
        }

        try {
            new IndexerDataset(registry,
                    getNodeFactoryFactory(),
                    getGraphServiceFactory(),
                    new File(getDatasetDir())
            ).index();
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        } finally {
            destroy();
        }
    }

    private static class DatasetRegistryWithStaticNamespaces implements DatasetRegistry {
        private final List<String> namepaces;
        private final DatasetRegistry registry;

        public DatasetRegistryWithStaticNamespaces(List<String> namepaces, DatasetRegistry registry) {
            this.namepaces = namepaces;
            this.registry = registry;
        }

        @Override
        public Iterable<String> findNamespaces() throws DatasetRegistryException {
            return namepaces;
        }

        @Override
        public void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException {
            namepaces.forEach(namespaceConsumer);
        }

        @Override
        public Dataset datasetFor(String namespace) throws DatasetRegistryException {
            return registry.datasetFor(namespace);
        }
    }
}
