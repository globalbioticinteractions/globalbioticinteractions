package org.trophic.graph.service;

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
import org.trophic.graph.domain.TaxonImage;
import org.trophic.graph.domain.TaxonomyProvider;
import org.trophic.graph.data.taxon.OboParser;

import java.io.IOException;
import java.util.Iterator;

public class TaxonImageEnricher extends TaxonEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonImageEnricher.class);

    public TaxonImageEnricher(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    protected void enrichTaxonUsingMatch(String matchString) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        String queryPrefix = "START taxon = node:taxons('*:*') "
                + "MATCH " + matchString
                + "WHERE has(taxon.externalId) ";

        LOG.info("matching [" + matchString + "]...");

        ExecutionResult result = engine.execute(queryPrefix
                + "RETURN count(distinct taxon) as totalTaxons");
        Iterator<Long> totalAffectedTaxons = result.columnAs("totalTaxons");
        Long totalTaxons = totalAffectedTaxons.next();


        result = engine.execute(queryPrefix
                + "RETURN distinct taxon");
        Iterator<Node> taxon = result.columnAs("taxon");
        EOLTaxonImageService service = new EOLTaxonImageService();
        long count = 0;
        while (taxon.hasNext()) {
            Node taxonNode = taxon.next();
            count++;
            if (count % 10 == 0) {
                LOG.info("Attempted to enrich taxon [" + count + "] out of [" + totalTaxons + "] with images");
            }
            String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
            String externalId = (String) taxonNode.getProperty(Taxon.EXTERNAL_ID);
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            try {
                TaxonomyProvider taxonomyProvider = lookupProvider(externalId);
                if (taxonomyProvider != null) {
                    int lastColon = externalId.lastIndexOf(":");
                    TaxonImage taxonImage = service.lookupImageURLs(taxonomyProvider, externalId.substring(lastColon + 1, externalId.length()));
                    stopwatch.stop();
                    String responseTime = "(took " + stopwatch.getTime() + "ms)";
                    String msg = "for [" + taxonName + "] with externalId [" + externalId + "] in [" + service.getClass().getSimpleName() + "] " + responseTime;
                    if (taxonImage == null) {
                        LOG.info("no match found " + msg);
                    } else {
                        LOG.info("found match " + msg);
                        enrichNode(taxonNode, taxonImage);
                    }
                }

            } catch (IOException ex) {
                LOG.warn("failed to find a match for [" + taxonName + "] in [" + service.getClass().getSimpleName() + "]", ex);
            }
        }

    }

    private TaxonomyProvider lookupProvider(String externalId) {
        TaxonomyProvider taxonomyProvider = null;
        if (externalId.startsWith(WoRMSService.URN_LSID_PREFIX)) {
            taxonomyProvider = TaxonomyProvider.WORMS;
        } else if (externalId.startsWith(ITISService.URN_LSID_PREFIX)) {
            taxonomyProvider = TaxonomyProvider.ITIS;
        } else if (externalId.startsWith(OboParser.URN_LSID_PREFIX)) {
            taxonomyProvider = TaxonomyProvider.NCBI;
        } else if (externalId.startsWith(EOLTaxonImageService.EOL_LSID_PREFIX)) {
            taxonomyProvider = TaxonomyProvider.EOL;
        }
        return taxonomyProvider;
    }

    private void enrichNode(Node node, TaxonImage taxonImage) {
        Transaction transaction = graphDbService.beginTx();
        try {
            if (taxonImage.getImageURL() != null) {
                node.setProperty(Taxon.IMAGE_URL, taxonImage.getImageURL());
            }
            if (taxonImage.getThumbnailURL() != null) {
                node.setProperty(Taxon.THUMBNAIL_URL, taxonImage.getThumbnailURL());
            }
            if (taxonImage.getEOLPageId() != null) {
                node.setProperty(Taxon.EXTERNAL_ID, EOLTaxonImageService.EOL_LSID_PREFIX + taxonImage.getEOLPageId());
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

}
