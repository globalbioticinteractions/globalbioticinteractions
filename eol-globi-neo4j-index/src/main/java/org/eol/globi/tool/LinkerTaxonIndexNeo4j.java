package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.util.NodeIdCollector;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;

import java.util.concurrent.TimeUnit;

import static org.eol.globi.taxon.TaxonFuzzySearchIndexNeo4j.TAXON_NAME_SUGGESTIONS;

public class LinkerTaxonIndexNeo4j implements IndexerNeo4j {

    private final GraphServiceFactory factory;

    public LinkerTaxonIndexNeo4j(GraphServiceFactory factory) {
        this.factory = factory;
    }

    public LinkerTaxonIndexNeo4j(GraphServiceFactoryProxy factory, NodeIdCollector nodeIdCollectorNeo4j3) {
        this(factory);
    }

    @Override
    public void index() throws StudyImporterException {
        try (Transaction tx = factory.getGraphService().beginTx()) {
            tx.schema().awaitIndexOnline(TAXON_NAME_SUGGESTIONS, 10, TimeUnit.SECONDS);
            IndexDefinition indexByName = tx.schema().getIndexByName(TAXON_NAME_SUGGESTIONS);
            if (!indexByName.isNodeIndex()) {
                throw new StudyImporterException("index aint lookin' good");
            }
            tx.commit();
        }
    }
}
