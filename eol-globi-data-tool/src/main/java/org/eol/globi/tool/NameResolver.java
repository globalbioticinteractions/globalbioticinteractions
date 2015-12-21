package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.CorrectionService;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eol.globi.domain.PropertyAndValueDictionary.*;
import static org.eol.globi.domain.PropertyAndValueDictionary.STATUS_ID;

public class NameResolver {
    private static final Log LOG = LogFactory.getLog(NameResolver.class);

    private final GraphDatabaseService graphService;

    private final TaxonIndex taxonIndex;
    public static final List<String> KNOWN_BAD_NAMES = new ArrayList<String>() {
        {
            add("sp");
        }
    };

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }

    private Long batchSize = 100L;

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
        ExecutionEngine engine = new ExecutionEngine(graphService);
        LOG.info("name resolving started...");
        resolveNames(engine);
        LOG.info("name resolving complete.");

        LOG.info("building interaction indexes...");

        boolean hasMore = true;
        Long offset = 0L;
        while (hasMore) {
            final String query = "START study = node:studies('*:*') " +
                    "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon, specimen-[r]->otherSpecimen-[:CLASSIFIED_AS]->otherTaxon " +
                    "WITH taxon, otherTaxon, type(r) as interactType " +
                    "CREATE UNIQUE taxon-[otherR:interactType]->otherTaxon " +
                    "RETURN type(otherR)";
            final ExecutionResult execute = executeQueryPage(engine, offset, query);
            final ResourceIterator<Map<String, Object>> iterator = execute.iterator();
            hasMore = iterator.hasNext();
            while(iterator.hasNext()) {
                iterator.next();
                offset++;
            }
        }
        LOG.info("building interaction indexes done.");
    }

    public ExecutionResult executeQueryPage(ExecutionEngine engine, Long offset, String query) {
        return engine.execute(query + " SKIP {offset} LIMIT {batchSize}", getPagingParams(offset));
    }

    public void resolveNames(ExecutionEngine engine) {
        Long offset = 0L;
        boolean hasMore = true;
        while (hasMore) {
            String query = "START study = node:studies('*:*') " +
                                "MATCH study-[:COLLECTED]->specimen-[:ORIGINALLY_DESCRIBED_AS]->taxon, specimen-[?:CLASSIFIED_AS]->resolvedTaxon " +
                                "WHERE not(has(resolvedTaxon.name)) " +
                                "RETURN taxon." + EXTERNAL_ID + "? as `" + EXTERNAL_ID + "`" +
                                ", taxon." + NAME + "? as `" + NAME + "`" +
                                ", taxon." + STATUS_ID + "? as `" + STATUS_ID + "`" +
                                ", taxon." + STATUS_LABEL + "? as `" + STATUS_LABEL + "`" +
                                ", id(specimen) as `specimenId`";
            ExecutionResult result = executeQueryPage(engine, offset, query);
            Long count = 0L;
            StopWatch watch = new StopWatch();
            watch.start();
            ResourceIterator<Map<String, Object>> iterator = result.iterator();
            while (iterator.hasNext()) {
                Map<String, Object> row = iterator.next();
                String name = row.containsKey(NAME) ? (String) row.get(NAME) : null;
                String externalId = row.containsKey(EXTERNAL_ID) ? (String) row.get(EXTERNAL_ID) : null;
                String statusId = row.containsKey(STATUS_ID) ? (String) row.get(STATUS_ID) : null;
                String statusLabel = row.containsKey(STATUS_LABEL) ? (String) row.get(STATUS_LABEL) : null;
                Long specimenId = row.containsKey("specimenId") ? (Long) row.get("specimenId") : null;
                if (specimenId != null && seeminglyGoodNameOrId(name, externalId)) {
                    Specimen specimen = new Specimen(graphService.getNodeById(specimenId));
                    Taxon taxon = new TaxonImpl(name, externalId);
                    taxon.setStatus(new Term(statusId, statusLabel));
                    try {
                        TaxonNode resolvedTaxon = taxonIndex.getOrCreateTaxon(taxon);
                        if (resolvedTaxon != null) {
                            specimen.classifyAs(resolvedTaxon);
                        }
                    } catch (NodeFactoryException e) {
                        LOG.warn("failed to create taxon with name [" + taxon.getName() + "] and id [" + taxon.getExternalId() + "]", e);
                    }
                }
                count++;
            }
            iterator.close();

            if (count < batchSize) {
                hasMore = false;
            } else {
                offset += count;
            }
            if (count > 0) {
                watch.stop();
                LOG.info("resolved [" + count + "] names in " + getProgressMsg(count, watch));
            }
        }
    }

    public HashMap<String, Object> getPagingParams(Long offset) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("offset", offset);
        params.put("batchSize", batchSize);
        return params;
    }

    public static boolean seeminglyGoodNameOrId(String name, String externalId) {
        return externalId != null || (name != null && name.length() > 1 && !KNOWN_BAD_NAMES.contains(name));
    }

    public String getProgressMsg(Long count, StopWatch watch) {
        double totalTimeMins = watch.getTime() / (1000 * 60.0);
        return String.format("[%.1f] taxon/min over [%.1f] min", (count / totalTimeMins), totalTimeMins);

    }
}
