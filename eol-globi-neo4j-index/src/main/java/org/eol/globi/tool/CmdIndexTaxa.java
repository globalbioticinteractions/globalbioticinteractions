package org.eol.globi.tool;

import org.eol.globi.data.NonResolvingTaxonIndex;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.util.NodeIdCollectorImpl;
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
        final TaxonIndex taxonIndex = new NonResolvingTaxonIndex(getGraphServiceFactory().getGraphService());
        final IndexerNeo4j nameResolver = new NameResolver(getGraphServiceFactory(), taxonIndex);
        final IndexerNeo4j taxonInteractionIndexer = new TaxonInteractionIndexer(getGraphServiceFactory(), new NodeIdCollectorImpl());
        index(nameResolver, taxonInteractionIndexer);
    }

    private void index(IndexerNeo4j nameResolver, IndexerNeo4j taxonInteractionIndexer) {
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
