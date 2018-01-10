package org.eol.globi.export;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterTaxaDistinct extends ExporterTaxa {

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            // only write the taxa once, because they are unique across studies...
            exportAllDistinctTaxa(writer, ((StudyNode)study).getUnderlyingNode().getGraphDatabase());
        }
    }

    private void exportAllDistinctTaxa(Writer writer, GraphDatabaseService graphDatabase) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDatabase);
        ExecutionResult results = engine.execute("START taxon = node:taxons('*:*') " +
                "MATCH taxon<-[:CLASSIFIED_AS]-specimen " +
                "WHERE has(taxon.externalId) AND taxon.externalId <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "AND has(taxon.name) AND taxon.name <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "RETURN distinct(taxon)" +
                ", taxon.name as scientificName" +
                ", taxon.path? as path" +
                ", taxon.pathNames? as pathNames" +
                ", taxon.rank? as rank" +
                ", taxon.externalId as taxonId");

        Map<String, String> row = new HashMap<String, String>();
        for (Map<String, Object> result : results) {
            resultsToRow(row, result);
            writeProperties(writer, row);
            row.clear();
        }
    }


}
