package org.eol.globi.export;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
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
        if (specimenNode != null) {
            Iterable<Relationship> relationships = specimenNode.getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
            Iterator<Relationship> iterator = relationships.iterator();
            if (iterator.hasNext()) {
                Relationship classifiedAs = iterator.next();
                if (classifiedAs != null) {
                    Node taxonNode = classifiedAs.getEndNode();
                    if (taxonNode.hasProperty(NodeBacked.EXTERNAL_ID)) {
                        String taxonId1 = (String) taxonNode.getProperty(NodeBacked.EXTERNAL_ID);
                        if (taxonId1 != null) {
                            properties.put(EOLDictionary.TAXON_ID, taxonId1);
                        }
                    }
                    if (taxonNode.hasProperty(Taxon.NAME)) {
                        String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
                        if (taxonName != null) {
                            properties.put(EOLDictionary.SCIENTIFIC_NAME, taxonName);
                        }
                    }
                }
            }
        }
        String scientificName = properties.get(EOLDictionary.SCIENTIFIC_NAME);
        String taxonId = properties.get(EOLDictionary.TAXON_ID);
        if (taxonId != null && scientificName != null) {
            taxa.put(taxonId, scientificName);
        }
        properties.clear();
    }

}
