package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdGenerateReport;
import org.eol.globi.tool.CmdNeo4J;
import picocli.CommandLine;

@CommandLine.Command(
        name = "summarize",
        description = "generates summary reports for ",
        subcommands = {
                CmdGenerateReport.class,
        }
)
public class CmdSummarize extends CmdNeo4J {


    @Override
    public void run() {
        configureAndRun(new CmdGenerateReport());
    }


}
