package org.eol.globi.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.util.InteractUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.MapUtil;

public class IndexInteractions implements IndexerNeo4j {
    private static final Log LOG = LogFactory.getLog(IndexInteractions.class);

    private Integer batchSize;

    public IndexInteractions() {
        this(10000);
    }

    public IndexInteractions(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void index(GraphServiceFactory factory) {
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
