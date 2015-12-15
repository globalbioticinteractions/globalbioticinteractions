package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.CorrectionService;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Collection;
import java.util.Map;

public class NameResolver {
    private static final Log LOG = LogFactory.getLog(NameResolver.class);

    private final GraphDatabaseService graphService;

    private final TaxonIndex taxonIndex;

    public NameResolver(GraphDatabaseService graphService) {
        this(graphService, PropertyEnricherFactory.createTaxonEnricher(), new TaxonNameCorrector());
    }

    public NameResolver(GraphDatabaseService graphService, TaxonIndex index) {
        this.graphService = graphService;
        this.taxonIndex = index;
    }

    public NameResolver(GraphDatabaseService graphService, PropertyEnricher enricher, CorrectionService corrector) {
        this(graphService, new TaxonIndexImpl(enricher, corrector, graphService));
    }

    public void resolve() {
        LOG.info("name resolving started...");
        ExecutionEngine engine = new ExecutionEngine(graphService);
        ExecutionResult result = engine.execute("START study = node:studies('*:*') " +
                "MATCH study-[:COLLECTED]->specimen-[:ORIGINALLY_DESCRIBED_AS]->taxon " +
                "RETURN distinct(taxon.externalId?) as `externalId`, taxon.name? as `name`, collect(id(specimen)) as `specimenIds`");
        int count = 0;
        StopWatch watch = new StopWatch();
        watch.start();
        for (Map<String, Object> row : result) {
            String name = row.containsKey(PropertyAndValueDictionary.NAME) ? (String) row.get(PropertyAndValueDictionary.NAME) : null;
            String externalId = row.containsKey(PropertyAndValueDictionary.EXTERNAL_ID) ? (String) row.get(PropertyAndValueDictionary.EXTERNAL_ID) : null;
            Collection<Long> specimenIds = row.containsKey("specimenIds") ? (Collection<Long>) row.get("specimenIds") : null;
            if (specimenIds != null) {
                for (Long specimenId : specimenIds) {
                    Specimen specimen = new Specimen(graphService.getNodeById(specimenId));
                    Taxon taxon = new TaxonImpl(name, externalId);
                    try {
                        TaxonNode taxonNode = taxonIndex.getOrCreateTaxon(taxon);
                        if (taxonNode != null) {
                            specimen.classifyAs(taxonNode);
                        }
                    } catch (NodeFactoryException e) {
                        LOG.warn("failed to create taxon with name [" + taxon.getName() + "] and id [" + taxon.getExternalId() + "]", e);
                    }
                }
            }

            if (count % 100 == 0) {
                if (count > 0) {
                    LOG.info("name resolving update: resolved [" + count + "] names at " + getProgressMsg(count, watch));
                }
            }
            count++;
        }
        LOG.info("name resolving completed in " + getProgressMsg(count, watch));

        LOG.info("building interaction indexes...");
        engine.execute("START study = node:studies('*:*') " +
                "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon, specimen-[r]->otherSpecimen-[:CLASSIFIED_AS]->otherTaxon " +
                "WITH taxon, otherTaxon, type(r) as interactType " +
                "CREATE UNIQUE taxon-[otherR:interactType]->otherTaxon " +
                "RETURN type(otherR)");
        LOG.info("building interaction indexes done.");
    }

    public String getProgressMsg(int count, StopWatch watch) {
        double totalTimeMins = 1000 * 60.0 / watch.getTime();
        return String.format("[%.1f] taxon/min over [%.1f] min", (count / totalTimeMins), watch.getTime() / (1000 * 60.0));
    }
}
