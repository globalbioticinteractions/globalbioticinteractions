package org.eol.globi.tool;

import org.eol.globi.data.NonResolvingTaxonIndexNeo4j3;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.taxon.NonResolvingTaxonIndexNeo4j2;
import org.eol.globi.taxon.TaxonFuzzySearchIndex;
import org.eol.globi.util.NodeIdCollectorNeo4j2;
import org.eol.globi.util.NodeIdCollectorNeo4j3;
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
        if ("2".equals(getNeo4jVersion())) {
            final TaxonIndex taxonIndex = new NonResolvingTaxonIndexNeo4j2(getGraphServiceFactory().getGraphService());
            final IndexerNeo4j nameResolver = new NameResolver(getGraphServiceFactory(), new NodeIdCollectorNeo4j2(), taxonIndex);
            final IndexerNeo4j taxonInteractionIndexer = new TaxonInteractionIndexer(getGraphServiceFactory(), new NodeIdCollectorNeo4j2());
            index(nameResolver, taxonInteractionIndexer);
        } else {
            final TaxonIndex taxonIndex = new NonResolvingTaxonIndexNeo4j3(getGraphServiceFactory().getGraphService());
            final IndexerNeo4j nameResolver = new NameResolver(getGraphServiceFactory(), new NodeIdCollectorNeo4j3(), taxonIndex);
            final IndexerNeo4j taxonInteractionIndexer = new TaxonInteractionIndexer(getGraphServiceFactory(), new NodeIdCollectorNeo4j3());
            index(nameResolver, taxonInteractionIndexer);
        }


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
