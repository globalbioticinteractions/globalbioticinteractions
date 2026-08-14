package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.taxon.FuzzyTaxonNameIndexNeo4j;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;

import java.util.concurrent.TimeUnit;

import static org.eol.globi.taxon.FuzzyTaxonNameIndexNeo4j.TAXON_NAME_SUGGESTIONS;

public class LinkerTaxonIndexNeo4j implements IndexerNeo4j {

    private final GraphServiceFactory factory;
    private FuzzyTaxonNameIndexNeo4j fuzzySearchIndex = null;

    public LinkerTaxonIndexNeo4j(GraphServiceFactory factory) {
        this.factory = factory;
    }

    @Override
    public void index() throws StudyImporterException {
        lazyInit();

        try (Transaction tx = factory.getGraphService().beginTx()) {
            tx.schema().awaitIndexOnline(TAXON_NAME_SUGGESTIONS, 10, TimeUnit.SECONDS);
            IndexDefinition indexByName = tx.schema().getIndexByName(TAXON_NAME_SUGGESTIONS);
            if (!indexByName.isNodeIndex()) {
                throw new StudyImporterException("expected index [" + TAXON_NAME_SUGGESTIONS + "to be a node index");
            }
            tx.commit();
        }
    }

    private void lazyInit() {
        if (fuzzySearchIndex == null) {
            fuzzySearchIndex = new FuzzyTaxonNameIndexNeo4j(factory.getGraphService());
        }
    }
}
