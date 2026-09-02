package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.util.InteractUtil;

public class IndexInteractionsNeo4j implements IndexerNeo4j {

    private final GraphServiceFactory graphServiceFactory;

    public IndexInteractionsNeo4j(GraphServiceFactory factory) {
        graphServiceFactory = factory;
    }


    @Override
    public void index() throws StudyImporterException {
        graphServiceFactory.getGraphService().executeTransactionally(
                "MATCH (dataset:Dataset)<-[:IN_DATASET]-(study:Reference)-[:REFUTES|SUPPORTS]->(specimen:Specimen)<-[:!HAS_PARTICIPANT]-() " +
                        "WITH specimen, study, dataset " +
                        "CALL (specimen, study, dataset) { " +
                        "  MATCH (specimen)-[i:" + InteractUtil.allInteractionsCypherClause() + "]->(otherSpecimen:Specimen)<-[:REFUTES|SUPPORTS]-(study) " +
                        "  WHERE i.inverted IS NULL " +
                        "  MERGE (specimen)<-[:HAS_PARTICIPANT]-(interaction:Interaction)-[:DERIVED_FROM]->(study) " +
                        "  MERGE (interaction)-[:HAS_PARTICIPANT]->(otherSpecimen) " +
                        "  MERGE (interaction)-[:ACCESSED_AT]->(dataset) " +
                        "  RETURN id(interaction) AS x " +
                        "}" +
                        "IN TRANSACTIONS OF 10000 ROWS " +
                        "RETURN count(x)");
    }
}
