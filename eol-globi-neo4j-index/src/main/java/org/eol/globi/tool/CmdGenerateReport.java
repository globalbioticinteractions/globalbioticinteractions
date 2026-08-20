package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeIdCollectorImpl;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.RelationshipListener;
import org.globalbioticinteractions.dataset.Dataset;
import org.mapdb.DB;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@CommandLine.Command(
        name = "report",
        description = "Generates reports/metadata of indexed datasets."
)
public class CmdGenerateReport extends CmdNeo4J {
    private static final Logger LOG = LoggerFactory.getLogger(CmdGenerateReport.class);

    private static final String GLOBI_COLLECTION_NAME = "Global Biotic Interactions";

    private CacheService cacheService = null;

    public void run() {
        run(LOG);
    }

    public void run(Logger log) {

        log.info("report for collection generating ...");

        String collectionReportMatcher = "  (r:Report { collection: 'Global Biotic Interactions' }) ";
        getGraphDb().executeTransactionally(
                metricsQuery(collectionReportMatcher)
        );

        getGraphDb().executeTransactionally(
                taxonMetricsQuery(collectionReportMatcher)
        );

        log.info("report for collection done.");

        String datasetReportMatcher = "  (r:Report { sourceId: 'globi:' + dataset.namespace }) ";
        log.info("report for datasets generating ...");
        getGraphDb().executeTransactionally(
                wrapWithDatasetContext(metricsQuery(datasetReportMatcher))
        );

        getGraphDb().executeTransactionally(
                wrapWithDatasetContext(taxonMetricsQuery(datasetReportMatcher))
        );

        log.info("report for datasets done.");
    }

    private static String wrapWithDatasetContext(String subquery) {
        return "MATCH (dataset:Dataset) WITH dataset CALL(dataset) { " +
                subquery +
                "} return r";
    }


    private static String taxonMetricsQuery(String collectionReportMatcher) {
        return "MATCH " +
                taxonMatchers() +
                "WITH " +
                "  COUNT(DISTINCT(resolvedTaxon)) as nTaxa, " +
                "  COUNT(DISTINCT(unresolvedTaxon)) as nTaxaNoMatch " +
                "MERGE " +
                collectionReportMatcher +
                setTaxonMetrics() +
                "RETURN r ";
    }

    private static String metricsQuery(String collectionReportMatcher) {
        return "MATCH " +
                interactionMatchers() + " " +
                "WITH " +
                "  COUNT(DISTINCT(r)) as nInteractions, " +
                "  COUNT(DISTINCT(study)) as nStudies, " +
                "  COUNT(DISTINCT(dataset)) as nDatasets " +
                "MERGE " +
                collectionReportMatcher +
                setReportMetrics2() +
                "RETURN r ";
    }

    private static String taxonMatchers() {
        return "  (resolvedTaxon:Taxon)<-[:CLASSIFIED_AS]-(:Specimen), " +
                " (unresolvedTaxon:Taxon)<-[:CLASSIFIED_AS]-(:Specimen) " +
                "WHERE resolvedTaxon.path IS NOT NULL AND unresolvedTaxon.path IS NULL ";
    }

    private static String interactionMatchers() {
        return "  (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(specimen:Specimen)-[r]->(:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "  (dataset:Dataset)<-[:IN_DATASET]-(study:Reference)-[:COLLECTED]->(specimen:Specimen) WHERE r.inverted IS NOT NULL";
    }

    private static String setReportMetrics2() {
        return "ON CREATE " + setReportMetrics() +
                "ON MATCH " + setReportMetrics();
    }

    private static String setTaxonMetrics() {
        return "ON CREATE " +
                "  SET r.nTaxa = nTaxa, " +
                "  r.nTaxaNoMatch = nTaxaNoMatch " +
                "ON MATCH " +
                "  SET r.nTaxa = nTaxa, " +
                "  r.nTaxaNoMatch = nTaxaNoMatch ";
    }

    private static String setReportMetrics() {
        return "  SET r.nInteractions = nInteractions" +
                ", r.nStudies = nStudies " +
                ", r.nDatasets = nDatasets " +
                ", r.nSources = nDatasets ";
    }

    private GraphDatabaseService getGraphDb() {
        return getGraphServiceFactory().getGraphService();
    }

}
