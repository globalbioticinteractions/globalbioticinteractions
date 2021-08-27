package org.eol.globi.tool;

import org.apache.commons.cli.CommandLine;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.globalbioticinteractions.dataset.DatasetRegistry;

public class CmdIndexDatasets implements Cmd {


    private final CommandLine cmdLine;
    private final NodeFactoryFactory nodeFactoryFactory;
    private final GraphServiceFactory graphServiceFactory;

    public CmdIndexDatasets(CommandLine cmdLine, NodeFactoryFactory nodeFactoryFactory, GraphServiceFactory factory) {
        this.cmdLine = cmdLine;
        this.nodeFactoryFactory = nodeFactoryFactory;
        this.graphServiceFactory = factory;
    }

    @Override
    public void run() throws StudyImporterException {
        String cacheDir = cmdLine == null
                ? "target/datasets"
                : cmdLine.getOptionValue(CmdOptionConstants.OPTION_DATASET_DIR, "target/datasets");

        DatasetRegistry registry = DatasetRegistryUtil.getDatasetRegistry(cacheDir);
        new IndexerDataset(registry, nodeFactoryFactory).index(graphServiceFactory);
    }
}
