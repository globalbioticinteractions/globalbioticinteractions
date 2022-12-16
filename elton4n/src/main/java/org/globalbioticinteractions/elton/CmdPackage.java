package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdExport;
import org.eol.globi.tool.CmdExportInteractionsTSV;
import org.eol.globi.tool.CmdExportNeo4J;
import picocli.CommandLine;

@CommandLine.Command(
        name = "package",
        description = "package interaction datasets into data products"
)
public class CmdPackage extends CmdExportNeo4J {

    @Override
    public void run() {
        configAndRun(new CmdExportInteractionsTSV());
        configAndRun(new CmdExport());
    }

    private void configAndRun(CmdExportNeo4J cmdExportInteractionsTSV) {
        cmdExportInteractionsTSV.setBaseDir(getBaseDir());
        configureAndRun(cmdExportInteractionsTSV);
    }

}
