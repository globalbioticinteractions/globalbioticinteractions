package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdGenerateReportNeo4j2;
import org.eol.globi.tool.CmdIndexTaxa;
import org.eol.globi.tool.CmdIndexTaxonStrings;
import org.eol.globi.tool.CmdInterpretTaxa;
import org.eol.globi.tool.CmdNeo4J;
import picocli.CommandLine;

@CommandLine.Command(
        name = "link",
        description = "link compiled interaction datasets",
        subcommands = {
                CmdInterpretTaxa.class,
                CmdIndexTaxa.class,
                CmdIndexTaxonStrings.class,
                CmdGenerateReportNeo4j2.class,
        }
)
public class CmdLinkNeo4j2 extends CmdNeo4J {


    @Override
    public void run() {
        configureAndRun(new CmdInterpretTaxa());
        configureAndRun(new CmdIndexTaxa());
        configureAndRun(new CmdIndexTaxonStrings());
        configureAndRun(new CmdGenerateReportNeo4j2());
    }



}
