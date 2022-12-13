package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
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
            new GraphExporterInteractionsTSVImpl()
                    .export(
                            getGraphServiceFactory().getGraphService(), new File(getBaseDir())
                    );
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }
}
