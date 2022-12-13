package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.export.GraphExporterImpl;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "package",
        description = "Export and package GloBI data products."
)
public class CmdExport extends CmdExportNeo4J {

    @Override
    public void run() {
        try {
            new GraphExporterImpl().export(
                            getGraphServiceFactory().getGraphService(),
                            new File(getBaseDir())
                    );
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }
}
