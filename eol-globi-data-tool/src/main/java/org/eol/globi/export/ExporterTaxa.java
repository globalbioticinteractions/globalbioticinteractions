package org.eol.globi.export;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterTaxa extends ExporterBase {

    protected String[] getFields() {
        return new String[]{
                EOLDictionary.TAXON_ID,
                EOLDictionary.SCIENTIFIC_NAME,
                EOLDictionary.PARENT_NAME_USAGE_ID,
                EOLDictionary.KINGDOM,
                EOLDictionary.PHYLUM,
                EOLDictionary.CLASS,
                EOLDictionary.ORDER,
                EOLDictionary.FAMILY,
                EOLDictionary.GENUS,
                EOLDictionary.TAXON_RANK,
                EOLDictionary.FURTHER_INFORMATION_URL,
                EOLDictionary.TAXONOMIC_STATUS,
                EOLDictionary.TAXON_REMARKS,
                EOLDictionary.NAME_PUBLISHED_IN,
                EOLDictionary.REFERENCE_ID
        };
    }

    @Override
    protected String getRowType() {
        return "http://rs.tdwg.org/dwc/terms/Taxon";
    }

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            // only write the taxa once, because they are unique across studies...
            exportAllDistinctTaxa(writer, study.getUnderlyingNode().getGraphDatabase());
        }
    }

    private void exportAllDistinctTaxa(Writer writer, GraphDatabaseService graphDatabase) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDatabase);
        ExecutionResult results = engine.execute("START taxon = node:taxons('*:*') " +
                "WHERE has(taxon.externalId) AND taxon.externalId <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "RETURN distinct(taxon), taxon.name as scientificName, taxon.externalId as taxonId");

        Map<String, String> properties = new HashMap<String, String>();
        for (Map<String, Object> result : results) {
            properties.put(EOLDictionary.TAXON_ID, (String) result.get("taxonId"));
            properties.put(EOLDictionary.SCIENTIFIC_NAME, (String) result.get("scientificName"));
            writeProperties(writer, properties);
            properties.clear();
        }
    }


}
