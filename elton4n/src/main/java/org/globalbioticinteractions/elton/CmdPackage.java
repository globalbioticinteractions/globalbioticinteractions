package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdExportInteractionsTSV;
import org.eol.globi.tool.CmdExportNeo4j2;
import org.eol.globi.tool.CmdExportNeo4J;
import org.eol.globi.tool.CmdNeo4J;
import picocli.CommandLine;

import java.util.stream.Stream;

@CommandLine.Command(
        name = "package",
        description = "package interaction datasets into data products"
)
public class CmdPackage extends CmdExportNeo4J {

    @Override
    public void run() {
        try {
            Stream
                    .of(new CmdExportInteractionsTSV(), new CmdExportNeo4j2())
                    .forEach(this::configAndRun);
        } finally {
            destroy();
        }
    }

    private void configAndRun(CmdNeo4J cmd) {
        cmd.setBaseDir(getBaseDir());
        configureAndRun(cmd);
    }

}
