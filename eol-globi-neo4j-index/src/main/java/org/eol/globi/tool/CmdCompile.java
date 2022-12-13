package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import picocli.CommandLine;

@CommandLine.Command(
        name = "compile",
        aliases = {"import"},
        description = "compile and import datasets into Neo4J"
)
public class CmdCompile extends CmdNeo4J {

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_DATASET_DIR},
            defaultValue = "./datasets",
            description = "location of Elton tracked datasets"
    )
    private String datasetDir;


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
