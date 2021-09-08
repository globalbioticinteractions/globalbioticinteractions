package org.eol.globi.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.util.InteractUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.MapUtil;

public class IndexInteractions implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(IndexInteractions.class);
    private final GraphServiceFactory factory;

    private Integer batchSize;

    public IndexInteractions(GraphServiceFactory factory) {
        this(factory, 10000);
    }

    public IndexInteractions(GraphServiceFactory factory, int batchSize) {
        this.batchSize = batchSize;
        this.factory = factory;
    }

    @Override
    public void index() {
        final GraphDatabaseService graphDb = factory.getGraphService();
        LinkProgress progress = new LinkProgress(LOG::info, 10);
        progress.start();

        boolean done;
        do {
            Result result = graphDb.execute("CYPHER 2.3 START dataset = node:datasets('*:*')\n" +
                    "MATCH dataset<-[:IN_DATASET]-study-[:REFUTES|SUPPORTS]->specimen\n" +
                    "WHERE not(specimen<-[:HAS_PARTICIPANT]-())\n" +
                    "WITH specimen, study, dataset LIMIT {batchSize}\n" +
                    "MATCH specimen-[i:" + InteractUtil.allInteractionsCypherClause() + "]->otherSpecimen\n" +
                    "WHERE not(exists(i.inverted))\n" +
                    "CREATE specimen<-[:HAS_PARTICIPANT]-interaction-[:DERIVED_FROM]->study" +
                    ", interaction-[:HAS_PARTICIPANT]->otherSpecimen " +
                    ", interaction-[:ACCESSED_AT]->dataset\n" +
                    "RETURN id(interaction)", MapUtil.map("batchSize", this.batchSize));
            done = !result.hasNext();
            result.close();
            progress.progress();
        } while (!done);
    }

}
