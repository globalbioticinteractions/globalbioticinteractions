package org.eol.globi.export;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class EOLExporterTaxa extends EOLExporterBase {

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
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        Map<String, String> taxa = new HashMap<String, String>();
        Map<String, String> properties = new HashMap<String, String>();

        Iterable<Relationship> specimens = study.getSpecimens();
        for (Relationship collectedRel : specimens) {
            Node specimenNode = collectedRel.getEndNode();
            addTaxa(taxa, properties, specimenNode);

            Iterable<Relationship> interactRelationships = specimenNode.getRelationships(Direction.OUTGOING, InteractType.values());
            if (interactRelationships.iterator().hasNext()) {
                for (Relationship interactRel : interactRelationships) {
                    addTaxa(taxa, properties, interactRel.getEndNode());
                }
            }
        }

        for (Map.Entry<String, String> idName : taxa.entrySet()) {
            properties.put(EOLDictionary.TAXON_ID, idName.getKey());
            properties.put(EOLDictionary.SCIENTIFIC_NAME, idName.getValue());
            writeProperties(writer, properties);
            properties.clear();
        }

    }

    private void addTaxa(Map<String, String> taxa, Map<String, String> properties, Node specimenNode) {
        addTaxonInfo(properties, specimenNode);
        String scientificName = properties.get(EOLDictionary.SCIENTIFIC_NAME);
        String taxonId = properties.get(EOLDictionary.TAXON_ID);
        if (taxonId != null && scientificName != null) {
            taxa.put(taxonId, scientificName);
        }
        properties.clear();
    }

}
