package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdGenerateReportNeo4j2;
import org.eol.globi.tool.CmdIndexTaxa;
import org.eol.globi.tool.CmdIndexTaxonStrings;
import org.eol.globi.tool.CmdInterpretTaxa;
import org.eol.globi.tool.CmdNeo4J;
import picocli.CommandLine;

@CommandLine.Command(
        name = "summarize",
        description = "generates summary reports for ",
        subcommands = {
                CmdGenerateReportNeo4j2.class,
        }
)
public class CmdSummarize extends CmdNeo4J {


    @Override
    public void run() {
        if ("2".equals(getNeo4jVersion())) {
            configureAndRun(new CmdGenerateReportNeo4j2());
        }
    }



}
