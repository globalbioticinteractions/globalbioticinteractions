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
import org.trophic.graph.data.OboImporter;
import org.trophic.graph.domain.Taxon;
import org.trophic.graph.domain.TaxonImage;
import org.trophic.graph.domain.TaxonomyProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TaxonImageEnricher extends BaseTaxonEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonImageEnricher.class);

    public TaxonImageEnricher(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    protected void enrichTaxonUsingMatch(String matchString) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        String queryPrefix = "START study = node:studies('*:*') "
                + "MATCH " + matchString
                + "WHERE has(taxon.externalId) ";

        LOG.info("matching [" + matchString + "]...");

        ExecutionResult result = engine.execute(queryPrefix
                + "RETURN distinct taxon");
        Iterator<Node> taxon = result.columnAs("taxon");
        Iterable<Node> objectIterable = IteratorUtil.asIterable(taxon);
        EOLTaxonImageService service = new EOLTaxonImageService();
        for (Node taxonNode : objectIterable) {
            String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
            String externalId = (String) taxonNode.getProperty(Taxon.EXTERNAL_ID);
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            try {
                TaxonomyProvider taxonomyProvider;
                if (externalId.startsWith(WoRMSService.URN_LSID_PREFIX)) {
                    taxonomyProvider = TaxonomyProvider.WORMS;
                } else if (externalId.startsWith(ITISService.URN_LSID_PREFIX)) {
                    taxonomyProvider = TaxonomyProvider.ITIS;
                } else if (externalId.startsWith(OboImporter.URN_LSID_PREFIX)) {
                    taxonomyProvider = TaxonomyProvider.NCBI;
                } else {
                    throw new UnsupportedOperationException(("found unsupported external id [" + externalId + "]"));
                }
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
            } catch (IOException ex) {
                LOG.warn("failed to find a match for [" + taxonName + "] in [" + service.getClass().getSimpleName() + "]", ex);
            }
        }

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
                node.setProperty(Taxon.EOL_PAGE_ID, taxonImage.getEOLPageId());
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

}
