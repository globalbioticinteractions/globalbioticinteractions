package org.globalbioticinteractions.elton;

import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.tool.CmdNeo4J;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdCreateTaxonIndexes extends CmdNeo4J {
    private final static Logger LOG = LoggerFactory.getLogger(CmdCreateTaxonIndexes.class);
    @Override
    public void run() {
        GraphDatabaseService graphService = getGraphServiceFactory().getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            Neo4jIndexUtil.createIndexIfNotExists(tx, NodeLabel.Taxon, PropertyAndValueDictionary.EXTERNAL_ID);
            Neo4jIndexUtil.createIndexIfNotExists(tx, NodeLabel.Taxon, PropertyAndValueDictionary.NAME);
            tx.commit();
        }
    }

}
