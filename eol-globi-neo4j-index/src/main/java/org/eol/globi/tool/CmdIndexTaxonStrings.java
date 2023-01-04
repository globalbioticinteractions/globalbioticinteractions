package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;


@CommandLine.Command(
        name = "indexTaxonStrings",
        description = "Interprets taxonomic strings using provided translation tables (taxonCache/Map)."
)
public class CmdIndexTaxonStrings extends CmdNeo4J {

    @Override
    public void run() {
        List<IndexerNeo4j> linkers = new ArrayList<>();
        linkers.add(new LinkerTaxonIndexNeo4j2(getGraphServiceFactory()));
        for (IndexerNeo4j linker : linkers) {
            try {
                new IndexerTimed(linker).index();
            } catch (StudyImporterException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
