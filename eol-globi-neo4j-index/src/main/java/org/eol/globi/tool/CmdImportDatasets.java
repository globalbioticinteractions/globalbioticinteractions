package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryImpl;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "compile",
        aliases = {"import"},
        description = "compile and import datasets into Neo4J"
)
public class CmdImportDatasets extends CmdNeo4J {

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_DATASET_DIR},
            description = "location of Elton tracked datasets"
    )
    private String datasetDir = "./datasets";


    @Override
    public void run() {
        DatasetRegistry registry = DatasetRegistryUtil.getDatasetRegistry(
                datasetDir,
                new ResourceServiceLocal(inStream -> inStream)
        );

        try {
            new IndexerDataset(registry, getNodeFactoryFactory(), getGraphServiceFactory())
                    .index();
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }

}
