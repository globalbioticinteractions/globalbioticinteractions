package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.export.GraphExporterImpl;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "package",
        description = "Export and package GloBI data products."
)
public abstract class CmdExportNeo4J extends CmdNeo4J {


    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_EXPORT_DIR},
            description = "location of neo4j graph.db"
    )
    private String baseDir = ".";

    public String getBaseDir() {
        return baseDir;
    }
}
