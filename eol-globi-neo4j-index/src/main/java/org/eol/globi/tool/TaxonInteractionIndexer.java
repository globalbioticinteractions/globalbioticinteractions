package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.InteractType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonInteractionIndexer implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonInteractionIndexer.class);
    private final GraphServiceFactory factory;

    TaxonInteractionIndexer(GraphServiceFactory factory) {
        this.factory = factory;
    }


    @Override
    public void index() throws StudyImporterException {
        InteractType[] values = InteractType.values();
        for (InteractType value : values) {
            makeInteractionShortCuts(value, "WHERE r.inverted IS NOT NULL ", ", inverted: r.inverted");
            makeInteractionShortCuts(value, "WHERE r.inverted IS NULL ", "");
        }
    }

    private void makeInteractionShortCuts(InteractType value, String invertedClause, String invertedPropertySet) {
        factory
                .getGraphService()
                .executeTransactionally(
                        "MATCH(source:Taxon)<-[:CLASSIFIED_AS]-(:Specimen)-[r:" + value.name() + "]->(:Specimen)-[:CLASSIFIED_AS]->(target:Taxon) " +
                                invertedClause +
                                "WITH source as source, target as target, r as r " +
                                "CALL(source, target, r) { " +
                                "  MERGE (source)-[ter:" + value.name() + " {iri: r.iri, label: r.label " + invertedPropertySet +"}] -> (target) return count(ter) as x " +
                                "} " +
                                "IN TRANSACTIONS OF 10000 ROWS RETURN count(x)");
    }
}
