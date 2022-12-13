package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;


@CommandLine.Command(
        name = "indexTaxonStrings",
        description = "Interprets taxonomic strings using provided translation tables (taxonCache/Map)."
)
public class CmdIndexTaxonStrings implements Cmd {

    private final GraphServiceFactory graphServiceFactory;

    public CmdIndexTaxonStrings(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    @Override
    public void run() throws StudyImporterException {
        List<IndexerNeo4j> linkers = new ArrayList<>();
        linkers.add(new LinkerTaxonIndex(graphServiceFactory));
        for (IndexerNeo4j linker : linkers) {
            new IndexerTimed(linker)
                    .index();
        }
    }
}
