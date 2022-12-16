package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdGenerateReport;
import org.eol.globi.tool.CmdIndexTaxa;
import org.eol.globi.tool.CmdIndexTaxonStrings;
import org.eol.globi.tool.CmdInterpretTaxa;
import org.eol.globi.tool.CmdNeo4J;
import picocli.CommandLine;

@CommandLine.Command(
        name = "link",
        description = "links compiled interaction datasets",
        subcommands = {
                CmdInterpretTaxa.class,
                CmdIndexTaxa.class,
                CmdIndexTaxonStrings.class,
                CmdGenerateReport.class,
        }
)
public class CmdLink extends CmdNeo4J {


    @Override
    public void run() {
        CmdInterpretTaxa cmd = new CmdInterpretTaxa();
        configureAndRun(cmd);
        configureAndRun(new CmdIndexTaxa());
        configureAndRun(new CmdIndexTaxonStrings());
        configureAndRun(new CmdGenerateReport());
    }



}
