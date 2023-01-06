package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.export.GraphExporterInteractionsTSVImpl;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "interactions",
        aliases = {"package-interactions-tsv"},
        description = "Exports indexed interactions into tsv."
)
public class CmdExportInteractionsTSV extends CmdExportNeo4J {

    @Override
    public void run() {
        try {
            new GraphExporterInteractionsTSVImpl(getNeo4jVersion())
                    .export(
                            getGraphServiceFactory().getGraphService(),
                            new File(getBaseDir()),
                            getNeo4jVersion()
                    );
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }
}
