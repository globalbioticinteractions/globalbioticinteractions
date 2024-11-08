package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.cache.ContentPathFactoryDepth0;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "compile",
        aliases = {"import"},
        description = "compile and import datasets into Neo4J"
)
public class CmdCompile extends CmdNeo4J {


    @Override
    public void run() {
        DatasetRegistry registry = DatasetRegistryUtil.getDatasetRegistry(
                getDatasetDir(),
                new ResourceServiceLocal(new InputStreamFactoryNoop()),
                new ContentPathFactoryDepth0()
        );

        try {
            new IndexerDataset(registry,
                    getNodeFactoryFactory(),
                    getGraphServiceFactory(),
                    new File(getDatasetDir())
            )
                    .index();
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }

}
