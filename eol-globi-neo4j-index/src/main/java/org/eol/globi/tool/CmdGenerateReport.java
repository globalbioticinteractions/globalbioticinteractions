package org.eol.globi.tool;

import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
        name = "report",
        description = "Generates reports/metadata of indexed datasets."
)
public class CmdGenerateReport extends CmdNeo4J {
    private static final Logger LOG = LoggerFactory.getLogger(CmdGenerateReport.class);

    private static final String GLOBI_COLLECTION_NAME = "Global Biotic Interactions";

    public void run() {
        run(LOG);
    }

    public void run(Logger log) {

        log.info("report for collection generating ...");

        String collectionReportMatcher = "  (r:Report { collection: '" + GLOBI_COLLECTION_NAME + "' }) ";
        String interactionMetricsQuery = interactionMetricsQuery(collectionReportMatcher);
        log.info("running [{}]", interactionMetricsQuery);
        getGraphDb().executeTransactionally(
                interactionMetricsQuery
        );

        String taxonMetricsQuery = taxonMetricsQuery(
                collectionReportMatcher,
                "NOT NULL",
                "nTaxa"
        );
        log.info("running [{}]", taxonMetricsQuery);
        getGraphDb().executeTransactionally(
                taxonMetricsQuery
        );

        String taxonMetricsQuery2 = taxonMetricsQuery(collectionReportMatcher,
                "NULL",
                "nTaxaNoMatch");
        log.info("running [{}]", taxonMetricsQuery);
        getGraphDb().executeTransactionally(
                taxonMetricsQuery2
        );

        log.info("report for collection done.");

        String datasetReportMatcher = "  (r:Report { sourceId: 'globi:' + dataset.namespace }) ";
        log.info("report for datasets generating ...");

        String datasetInteractionsQuery = wrapWithDatasetContext(interactionMetricsQuery(datasetReportMatcher));
        log.info("running [{}]", datasetInteractionsQuery);
        getGraphDb().executeTransactionally(
                datasetInteractionsQuery
        );

        String datasetTaxonResolvedQuery = wrapWithDatasetContext(taxonMetricsQuery(datasetReportMatcher,
                "NOT NULL",
                "nTaxa"));
        log.info("running [{}]", datasetTaxonResolvedQuery);
        getGraphDb().executeTransactionally(
                datasetTaxonResolvedQuery
        );

        String datasetTaxonUnresolvedQuery = wrapWithDatasetContext(taxonMetricsQuery(datasetReportMatcher,
                "NULL",
                "nTaxaNoMatch"));
        log.info("running [{}]", datasetTaxonUnresolvedQuery);
        getGraphDb().executeTransactionally(
                datasetTaxonUnresolvedQuery
        );

        log.info("report for datasets done.");
    }

    private static String wrapWithDatasetContext(String subquery) {
        return "MATCH (dataset:Dataset) WITH dataset CALL(dataset) { " +
                subquery +
                "} return r";
    }


    private static String taxonMetricsQuery(String collectionReportMatcher, String pathNullOrNotNull, String taxonMetrixName) {
        return "MATCH " +
                taxonMatchers(pathNullOrNotNull) +
                "WITH " +
                "  COUNT(DISTINCT(taxon)) as " + taxonMetrixName + " " +
                "MERGE " +
                collectionReportMatcher +
                setTaxonMetrics(taxonMetrixName) +
                "RETURN r ";
    }

    private static String interactionMetricsQuery(String collectionReportMatcher) {
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

    private static String taxonMatchers(String PathNullOrNotNull) {
        return "  (taxon:Taxon)<-[:CLASSIFIED_AS]-(:Specimen)<-[:COLLECTED]-(:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE taxon.path IS " + PathNullOrNotNull + " ";
    }

    private static String interactionMatchers() {
        return "  (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(specimen:Specimen)-[r]->(:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "  (dataset:Dataset)<-[:IN_DATASET]-(study:Reference)-[:COLLECTED]->(specimen:Specimen) WHERE r.inverted IS NOT NULL";
    }

    private static String setReportMetrics2() {
        return "ON CREATE " + setReportMetrics() +
                "ON MATCH " + setReportMetrics();
    }

    private static String setTaxonMetrics(String taxonMetricName) {
        return "ON CREATE " +
                "  SET  r." + taxonMetricName + " = " + taxonMetricName + " " +
                "ON MATCH " +
                "  SET  r." + taxonMetricName + " = " + taxonMetricName + " ";
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
