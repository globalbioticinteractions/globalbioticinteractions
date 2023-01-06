package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.export.GraphExporterImpl;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "package",
        description = "Export and package GloBI data products."
)
public class CmdExportNeo4j2 extends CmdExportNeo4J {

    @Override
    public void run() {
        try {
            new GraphExporterImpl().export(
                    getGraphServiceFactory().getGraphService(),
                    new File(getBaseDir()),
                    getNeo4jVersion()
            );
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }
}
