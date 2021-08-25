package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.DatasetImporterForRegistry;
import org.eol.globi.data.ParserFactoryLocal;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.service.DatasetLocal;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.globalbioticinteractions.dataset.DatasetUtil;

import java.util.Collection;

public class IndexerDataset implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerDataset.class);

    private final DatasetRegistry registry;
    private final NodeFactoryFactory nodeFactoryFactory;

    public IndexerDataset(DatasetRegistry registry, NodeFactoryFactory nodeFactoryFactory) {
        this.registry = registry;
        this.nodeFactoryFactory = nodeFactoryFactory;
    }

    @Override
    public void index(GraphServiceFactory graphServiceFactory) {
        NodeFactory nodeFactory = nodeFactoryFactory.create(graphServiceFactory.getGraphService());
        indexDatasets(
                this.registry,
                nodeFactory);
    }

    private static void indexDatasets(DatasetRegistry registry, NodeFactory nodeFactory) {
        try {
            final Collection<String> namespaces = registry.findNamespaces();

            String namespacelist = StringUtils.join(namespaces, CharsetConstant.SEPARATOR);
            LOG.info("found dataset namespaces: {" + namespacelist + "}");

            DatasetImporterForRegistry importer = new DatasetImporterForRegistry(new ParserFactoryLocal(), nodeFactory, registry);
            importer.setDatasetFilter(x -> !DatasetUtil.isDeprecated(x));
            importer.setDataset(new DatasetLocal(inStream -> inStream));
            importer.setLogger(new NullImportLogger());
            importer.importStudy();

        } catch (DatasetRegistryException | StudyImporterException e) {
            LOG.error("problem encountered while importing [" + DatasetImporterForRegistry.class.getName() + "]", e);
        }
    }

}
