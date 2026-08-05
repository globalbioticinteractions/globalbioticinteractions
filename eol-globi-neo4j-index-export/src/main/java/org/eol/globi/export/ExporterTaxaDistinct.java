package org.eol.globi.export;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.StudyNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.TreeMap;
import java.util.Map;

public class ExporterTaxaDistinct extends ExporterTaxa {


    public ExporterTaxaDistinct(GraphDatabaseService graphService) {
        super(graphService);
    }

    @Override
    public void doExportStudy(StudyNode study, ExportUtil.Appender writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            // only write the taxa once, because they are unique across studies...
            try(Transaction tx = getGraphService().beginTx()) {
                exportAllDistinctTaxa(writer, tx);
                tx.commit();
            }
        }
    }

    private void exportAllDistinctTaxa(ExportUtil.Appender writer, Transaction tx) throws IOException {
        Result results = tx.execute("CYPHER 2.3 START taxon = node:taxons('*:*') " +
                "MATCH taxon<-[:CLASSIFIED_AS]-specimen " +
                "WHERE exists(taxon.externalId) AND taxon.externalId <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "AND exists(taxon.name) AND taxon.name <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "RETURN distinct(taxon)" +
                ", taxon.name as scientificName" +
                ", taxon.path as path" +
                ", taxon.pathNames as pathNames" +
                ", taxon.rank as rank" +
                ", taxon.externalId as taxonId");

        Map<String, String> row = new TreeMap<String, String>();
        Map<String, Object> result;
        while (results.hasNext()) {
            result = results.next();
            resultsToRow(row, result);
            writeProperties(writer, row);
            row.clear();
        }
    }


}
