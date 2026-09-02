package org.eol.globi.export;

import org.eol.globi.domain.StudyNode;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.util.HashMap;

public class ExportTaxonMap implements StudyExporter {

    private final GraphDatabaseService graphService;

    public ExportTaxonMap(GraphDatabaseService graphService) {
        this.graphService = graphService;
    }

    @Override
    public void exportStudy(final StudyNode study, ExportUtil.Appender writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(writer);
        }
    }

    protected void doExport(ExportUtil.Appender writer) throws IOException {
        String query =
                "MATCH (study:Reference)-[:COLLECTED|REFUTES|SUPPORTS]->(specimen)-[:ORIGINALLY_DESCRIBED_AS]->(origTaxon), " +
                "(specimen)-[:CLASSIFIED_AS]->(taxon) " +
                "WITH distinct(origTaxon.name) as origName, origTaxon.externalId as origId, origTaxon.path as origPath, taxon " +
                "MATCH (taxon)-[:SAME_AS*0..1]->(linkedTaxon) " +
                "WHERE linkedTaxon.path IS NOT NULL " +
                "RETURN origId as providedTaxonId" +
                ", origName as providedTaxonName" +
                ", origPath as providedTaxonPath" +
                ", linkedTaxon.externalId as resolvedTaxonId" +
                ", linkedTaxon.name as resolvedTaxonName" +
                ", linkedTaxon.path as resolvedTaxonPath";

        ExportUtil.writeResults(
                writer,
                graphService,
                query,
                new HashMap<>(),
                true
        );
    }
}
