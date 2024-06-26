package org.eol.globi.export;

import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.RelationshipListener;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;
import java.util.TreeMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ExporterMeasurementOrFact extends ExporterBase {

    protected String[] getFields() {
        return new String[]{
                EOLDictionary.MEASUREMENT_ID,
                EOLDictionary.OCCURRENCE_ID,
                EOLDictionary.MEASUREMENT_OF_TAXON,
                EOLDictionary.ASSOCIATION_ID,
                "http://eol.org/schema/parentMeasurementID",
                EOLDictionary.MEASUREMENT_TYPE,
                EOLDictionary.MEASUREMENT_VALUE,
                EOLDictionary.MEASUREMENT_UNIT,
                EOLDictionary.MEASUREMENT_ACCURACY,
                "http://eol.org/schema/terms/statisticalMethod",
                EOLDictionary.MEASUREMENT_DETERMINED_DATE,
                EOLDictionary.MEASUREMENT_DETERMINED_BY,
                EOLDictionary.MEASUREMENT_METHOD,
                EOLDictionary.MEASUREMENT_REMARKS,
                EOLDictionary.SOURCE,
                EOLDictionary.BIBLIOGRAPHIC_CITATION,
                EOLDictionary.CONTRIBUTOR,
                EOLDictionary.REFERENCE_ID
        };
    }

    @Override
    protected String getRowType() {
        return "http://eol.org/schema/reference/MeasurementOrFact";
    }

    @Override
    public void doExportStudy(StudyNode study, ExportUtil.Appender writer, boolean includeHeader) throws IOException {
        Map<String, String> properties = new TreeMap<String, String>();

        AtomicReference<IOException> lastException = new AtomicReference<>();
        RelationshipListener handler = collectedRel -> {
            Node specimenNode = collectedRel.getEndNode();
            if (isSpecimenClassified(specimenNode)) {
                try {
                    writeMeasurements(writer, properties, specimenNode, collectedRel, study);
                } catch (IOException ex) {
                    lastException.set(ex);
                }
            }
        };
        NodeUtil.handleCollectedRelationships(
                new NodeTypeDirection(study.getUnderlyingNode()),
                handler);

        if (lastException.get() != null) {
            throw lastException.get();
        }
     }

    private void writeMeasurements(ExportUtil.Appender writer, Map<String, String> properties, Node specimenNode, Relationship collectedRel, StudyNode study) throws IOException {
        writeProperties(writer, properties, specimenNode, collectedRel, study);
    }

    private void writeProperties(ExportUtil.Appender writer, Map<String, String> properties, Node specimenNode, Relationship collectedRel, StudyNode study) throws IOException {
        if (specimenNode.hasProperty(SpecimenConstant.LENGTH_IN_MM)) {
            Object property = specimenNode.getProperty(SpecimenConstant.LENGTH_IN_MM);
            properties.put(EOLDictionary.MEASUREMENT_VALUE, property.toString());
            properties.put(EOLDictionary.MEASUREMENT_TYPE, "specimen length");
            properties.put(EOLDictionary.MEASUREMENT_ID, "globi:occur:length:" + specimenNode.getId());
            properties.put(EOLDictionary.MEASUREMENT_UNIT, "http://purl.obolibrary.org/obo/UO_0000016");
            properties.put(EOLDictionary.MEASUREMENT_OF_TAXON, "yes");
            addCommonProperties(properties, specimenNode, collectedRel, study);
            writeProperties(writer, properties);
        }

        if (specimenNode.hasProperty(SpecimenConstant.STOMACH_VOLUME_ML)) {
            Object property = specimenNode.getProperty(SpecimenConstant.STOMACH_VOLUME_ML);
            properties.put(EOLDictionary.MEASUREMENT_VALUE, property.toString());
            properties.put(EOLDictionary.MEASUREMENT_TYPE, "stomach volume");
            properties.put(EOLDictionary.MEASUREMENT_OF_TAXON, "yes");
            properties.put(EOLDictionary.MEASUREMENT_ID, "globi:occur:stomach_volume:" + specimenNode.getId());
            properties.put(EOLDictionary.MEASUREMENT_UNIT, "http://purl.obolibrary.org/obo/UO_0000098");
            addCommonProperties(properties, specimenNode, collectedRel, study);
            writeProperties(writer, properties);
        }

        if (specimenNode.hasProperty(SpecimenConstant.VOLUME_IN_ML)) {
            Object property = specimenNode.getProperty(SpecimenConstant.VOLUME_IN_ML);
            properties.put(EOLDictionary.MEASUREMENT_VALUE, property.toString());
            properties.put(EOLDictionary.MEASUREMENT_TYPE, "volume");
            properties.put(EOLDictionary.MEASUREMENT_ID, "globi:occur:volume:" + specimenNode.getId());
            properties.put(EOLDictionary.MEASUREMENT_UNIT, "http://purl.obolibrary.org/obo/UO_0000098");
            properties.put(EOLDictionary.MEASUREMENT_OF_TAXON, "yes");
            addCommonProperties(properties, specimenNode, collectedRel, study);
            writeProperties(writer, properties);
        }
    }

    private void addCommonProperties(Map<String, String> properties, Node specimenNode, Relationship collectedRel, StudyNode study) throws IOException {
        properties.put(EOLDictionary.SOURCE, study.getTitle());
        addCollectionDate(properties, collectedRel, EOLDictionary.MEASUREMENT_DETERMINED_DATE);
        properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:" + specimenNode.getId());
        properties.put(EOLDictionary.REFERENCE_ID, ExporterReferences.referenceIdForStudy(study));
    }
}
