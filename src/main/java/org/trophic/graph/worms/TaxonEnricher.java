package org.trophic.graph.worms;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;
import java.util.Iterator;

public class TaxonEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonEnricher.class);

    private GraphDatabaseService graphDbService;

    public TaxonEnricher(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
    }

    public void enrichTaxons() throws IOException {
        String predatorTaxons = "study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon ";
        enrichTaxonUsingMatch(predatorTaxons);
        String preyTaxons = "study-[:COLLECTED]->predator-[:ATE]->prey-[:CLASSIFIED_AS]->taxon ";
        enrichTaxonUsingMatch(preyTaxons);
    }

    private void enrichTaxonUsingMatch(String matchString) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        String queryPrefix = "START study = node:studies('*:*') "
                + "MATCH " + matchString
                + "WHERE not(has(taxon.externalId)) ";

        LOG.info("matching [" + matchString + "]...");

        ExecutionResult result = engine.execute(queryPrefix
                + "RETURN distinct taxon");
        Iterator<Node> taxon = result.columnAs("taxon");
        Iterable<Node> objectIterable = IteratorUtil.asIterable(taxon);
        WoRMSService service = new WoRMSService();
        for (Node taxonNode : objectIterable) {
            String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            try {
                String lsid = service.lookupLSIDByTaxonName(taxonName);
                stopwatch.stop();
                String responseTime = "(took " + stopwatch.getTime() + "ms)";
                if (lsid == null) {
                    LOG.info("no match found for [" + taxonName + "] in WoRMS " + responseTime);
                } else {
                    LOG.info("matched [" + taxonName + "] with LSID [" + lsid + "]" + responseTime);
                    enrichNode(taxonNode, lsid);
                }
            } catch (IOException ex) {
                LOG.warn("failed to find a match for [" + taxonName + "] in WoRMS", ex);
                service.shutdown();
                service = new WoRMSService();
            }
        }

        service.shutdown();
    }

    private void enrichNode(Node node, String lsid) {
        Transaction transaction = graphDbService.beginTx();
        try {
            node.setProperty(Taxon.EXTERNAL_ID, lsid);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

}
