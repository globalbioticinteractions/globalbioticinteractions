package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdExportInteractionsTSV;
import org.eol.globi.tool.CmdExportNeo4j2;
import org.eol.globi.tool.CmdExportNeo4J;
import picocli.CommandLine;

@CommandLine.Command(
        name = "package",
        description = "package interaction datasets into data products"
)
public class CmdPackage extends CmdExportNeo4J {

    @Override
    public void run() {
        if ("2".equals(getNeo4jVersion())) {
            configAndRun(new CmdExportInteractionsTSV());
            configAndRun(new CmdExportNeo4j2());
        } else {
            configAndRun(new CmdExportInteractionsTSV());
        }
    }

    private void configAndRun(CmdExportNeo4J cmdExportInteractionsTSV) {
        cmdExportInteractionsTSV.setBaseDir(getBaseDir());
        configureAndRun(cmdExportInteractionsTSV);
    }

}
