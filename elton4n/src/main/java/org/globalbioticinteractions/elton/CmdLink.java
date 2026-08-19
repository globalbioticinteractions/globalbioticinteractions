package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdFullTextTaxonIndex;
import org.eol.globi.tool.CmdInterpretTaxa;
import org.eol.globi.tool.CmdNeo4J;
import org.eol.globi.tool.CmdTaxonToTaxonInteractionIndex;
import picocli.CommandLine;

import java.util.stream.Stream;

@CommandLine.Command(
        name = "link",
        description = "link compiled interaction datasets",
        subcommands = {
                CmdInterpretTaxa.class,
                CmdFullTextTaxonIndex.class,
        }
)
public class CmdLink extends CmdNeo4J {


    @Override
    public void run() {
        try {
            Stream.of(
                            new CmdFullTextTaxonIndex(),
                            new CmdCreateTaxonIndexes(),
                            new CmdCreateLocationIndexes(),
                            new CmdInterpretTaxa(),
                            new CmdTaxonToTaxonInteractionIndex()

                    )
                    .forEach(this::configureAndRun);
        } finally {
            //destroy();
        }
    }


}
