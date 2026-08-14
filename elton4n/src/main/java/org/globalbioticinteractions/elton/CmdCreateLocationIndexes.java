package org.globalbioticinteractions.elton;

import org.eol.globi.data.NodeLabel;
import org.eol.globi.tool.CmdNeo4J;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.IndexType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.eol.globi.domain.LocationConstant.LATITUDE;
import static org.eol.globi.domain.LocationConstant.LONGITUDE;

public class CmdCreateLocationIndexes extends CmdNeo4J {
    private final static Logger LOG = LoggerFactory.getLogger(CmdCreateLocationIndexes.class);

    @Override
    public void run() {
        GraphDatabaseService graphService = getGraphServiceFactory().getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            createIndexIfNotExists(tx);
            tx.commit();
        }
    }

    private static void createIndexIfNotExists(Transaction tx) {
        List<String> taxonIndexNames = new ArrayList<>();
        Iterable<IndexDefinition> taxonIndexes = tx.schema().getIndexes(NodeLabel.Taxon);
        taxonIndexes.forEach(i -> taxonIndexNames.add(i.getName()));
        String indexName = NodeLabel.Location.name() + LATITUDE + "_" + LONGITUDE;
        if (taxonIndexNames.contains(indexName)) {
            LOG.info("found existing index [{}]", indexName);
        } else {
            tx.schema()
                    .indexFor(NodeLabel.Location)
                    .withIndexType(IndexType.POINT)
                    .on(LATITUDE)
                    .on(LONGITUDE)
                    .withName(indexName)
                    .create();
            LOG.info("created index [{}]", indexName);
        }
    }

    private static String indexNameFor(String propertyName) {
        return NodeLabel.Taxon.name() + propertyName;
    }
}
