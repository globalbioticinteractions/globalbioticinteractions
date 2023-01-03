package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.taxon.NonResolvingTaxonIndexNeo4j2;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;


@CommandLine.Command(
        name = "indexTaxa",
        description = "Creates neo4j index for (interpreted) taxonomic names."
)
public class CmdIndexTaxa extends CmdNeo4J {

    @Override
    public void run() {
        final NonResolvingTaxonIndexNeo4j2 taxonIndex = new NonResolvingTaxonIndexNeo4j2(getGraphServiceFactory().getGraphService());
        final IndexerNeo4j nameResolver = new NameResolver(getGraphServiceFactory(), taxonIndex);
        final IndexerNeo4j taxonInteractionIndexer = new TaxonInteractionIndexer(getGraphServiceFactory());

        List<IndexerNeo4j> indexers = Arrays.asList(nameResolver, taxonInteractionIndexer);
        for (IndexerNeo4j indexer : indexers) {
            try {
                indexer.index();
            } catch (StudyImporterException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
