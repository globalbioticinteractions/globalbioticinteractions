package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.globalbioticinteractions.dataset.DatasetRegistry;

public class CmdImportDatasets implements Cmd {


    private final NodeFactoryFactory nodeFactoryFactory;
    private final GraphServiceFactory graphServiceFactory;
    private final String datasetDir;

    public CmdImportDatasets(NodeFactoryFactory nodeFactoryFactory,
                             GraphServiceFactory factory,
                             String datasetDir) {
        this.nodeFactoryFactory = nodeFactoryFactory;
        this.graphServiceFactory = factory;
        this.datasetDir = datasetDir;
    }

    @Override
    public void run() throws StudyImporterException {
        DatasetRegistry registry = DatasetRegistryUtil.getDatasetRegistry(datasetDir);
        new IndexerDataset(registry, nodeFactoryFactory, graphServiceFactory).index();
    }

}
