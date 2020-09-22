package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.DatasetImporterForRegistry;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.ParserFactoryLocal;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.DatasetLocal;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.neo4j.graphdb.GraphDatabaseService;

public class IndexerDataset implements IndexerNeo4j {
    private static final Log LOG = LogFactory.getLog(IndexerDataset.class);

    private final DatasetRegistry registry;

    public IndexerDataset(DatasetRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void index(GraphServiceFactory graphService) {
        indexDatasets(graphService.getGraphService(), this.registry);
    }

    private static void indexDatasets(GraphDatabaseService graphService, DatasetRegistry registry) {
        NodeFactoryNeo4j factory = new NodeFactoryNeo4j(graphService);
        factory.setDoiResolver(new DOIResolverImpl());
        try {
            String namespacelist = StringUtils.join(registry.findNamespaces(), CharsetConstant.SEPARATOR);

            LOG.info("found dataset namespaces: {" + namespacelist + "}");

            DatasetImporter importer = new DatasetImporterForRegistry(new ParserFactoryLocal(), factory, registry);
            importer.setDataset(new DatasetLocal(inStream -> inStream));
            importer.setLogger(new NullImportLogger());
            importer.importStudy();
        } catch (StudyImporterException | DatasetRegistryException e) {
            LOG.error("problem encountered while importing [" + DatasetImporterForRegistry.class.getName() + "]", e);
        }
    }

}
