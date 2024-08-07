package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.DatasetImporterForRegistry;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.ParserFactoryLocal;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.globalbioticinteractions.dataset.DatasetUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class IndexerDataset implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerDataset.class);

    private final DatasetRegistry registry;
    private final NodeFactoryFactory nodeFactoryFactory;
    private final GraphServiceFactory graphServiceFactory;
    private final File cacheDir;

    public IndexerDataset(DatasetRegistry registry,
                          NodeFactoryFactory nodeFactoryFactory,
                          GraphServiceFactory graphServiceFactory,
                          File cacheDir) {
        this.registry = registry;
        this.nodeFactoryFactory = nodeFactoryFactory;
        this.graphServiceFactory = graphServiceFactory;
        this.cacheDir = cacheDir;
    }

    @Override
    public void index() throws StudyImporterException {
        GraphDatabaseService graphService = graphServiceFactory.getGraphService();
        NodeFactory nodeFactory;
        try (Transaction tx = graphService.beginTx();) {
            nodeFactory = nodeFactoryFactory.create(graphService, cacheDir);
            tx.success();
        }

        try {
            indexDatasets(
                    this.registry,
                    nodeFactory);
        } finally {
            if (nodeFactory != null) {
                try {
                    nodeFactory.close();
                } catch (Exception e) {
                    throw new StudyImporterException("failed to gracefully end index process", e);
                }
            }
        }
    }

    private static void indexDatasets(DatasetRegistry registry, NodeFactory nodeFactory) {
        try {
            final Iterable<String> namespaces = registry.findNamespaces();

            String namespacelist = StringUtils.join(namespaces, CharsetConstant.SEPARATOR);
            LOG.info("found dataset namespaces: {" + namespacelist + "}");

            DatasetImporterForRegistry importer = new DatasetImporterForRegistry(
                    new ParserFactoryLocal(IndexerDataset.class),
                    nodeFactory,
                    registry);
            importer.setDatasetFilter(x -> !DatasetUtil.isDeprecated(x));
            importer.setDataset(new DatasetLocal(new ResourceServiceLocal(new InputStreamFactoryNoop())));
            importer.setLogger(new NullImportLogger());
            importer.importStudy();

        } catch (DatasetRegistryException | StudyImporterException e) {
            LOG.error("problem encountered while importing [" + DatasetImporterForRegistry.class.getName() + "]", e);
        }
    }

}
