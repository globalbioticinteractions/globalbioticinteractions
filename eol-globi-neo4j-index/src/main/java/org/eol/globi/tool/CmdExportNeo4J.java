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


}
