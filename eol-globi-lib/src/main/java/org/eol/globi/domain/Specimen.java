package org.eol.globi.domain;

import org.eol.globi.service.TaxonUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.List;

public class Specimen extends NodeBacked {

    public static final String LENGTH_IN_MM = "lengthInMm";
    public static final String VOLUME_IN_ML = "volumeInMilliLiter";
    public static final String STOMACH_VOLUME_ML = "stomachVolumeInMilliLiter";
    public static final String DATE_IN_UNIX_EPOCH = "dateInUnixEpoch";
    public static final String BASIS_OF_RECORD_LABEL = "basisOfRecordLabel";
    public static final String BASIS_OF_RECORD_ID = "basisOfRecordId";
    public static final String LIFE_STAGE_LABEL = "lifeStageLabel";
    public static final String LIFE_STAGE_ID = "lifeStageId";
    public static final String PHYSIOLOGICAL_STATE_LABEL = "physiologicalStateLabel";
    private static final String PHYSIOLOGICAL_STATE_ID = "physiologicalStateId";
    public static final String BODY_PART_LABEL = "bodyPartLabel";
    public static final String BODY_PART_ID = "bodyPartId";
    public static final String TOTAL_COUNT = "totalNumberConsumed";
    public static final String TOTAL_COUNT_PERCENT = "totalNumberConsumedPercent";
    public static final String TOTAL_VOLUME_IN_ML = "totalVolumeInMl";
    public static final String TOTAL_VOLUME_PERCENT = "totalVolumePercent";
    public static final String FREQUENCY_OF_OCCURRENCE = "frequencyOfOccurrence";
    public static final String FREQUENCY_OF_OCCURRENCE_PERCENT = "frequencyOfOccurrencePercent";

    public Specimen(Node node) {
        super(node);
    }

    public Specimen(Node node, Double lengthInMm) {
        this(node);
        getUnderlyingNode().setProperty(PropertyAndValueDictionary.TYPE, Specimen.class.getSimpleName());
        if (null != lengthInMm) {
            getUnderlyingNode().setProperty(LENGTH_IN_MM, lengthInMm);
        }
    }

    @Override
    public String toString() {
        return String.format("[%s]", getUnderlyingNode().getProperty(PropertyAndValueDictionary.TYPE));
    }

    public Iterable<Relationship> getStomachContents() {
        return getUnderlyingNode().getRelationships(InteractType.ATE, Direction.OUTGOING);
    }

    public LocationNode getSampleLocation() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
        return singleRelationship == null ? null : new LocationNode(singleRelationship.getEndNode());
    }

    public void ate(Specimen specimen) {
        interactsWith(specimen, InteractType.ATE);
    }

    public void caughtIn(LocationNode sampleLocation) {
        if (null != sampleLocation) {
            createRelationshipTo(sampleLocation, RelTypes.COLLECTED_AT);
        }
    }

    public Season getSeason() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(RelTypes.CAUGHT_DURING, Direction.OUTGOING);
        return singleRelationship == null ? null : new Season(singleRelationship.getEndNode());
    }

    public void caughtDuring(Season season) {
        createRelationshipTo(season, RelTypes.CAUGHT_DURING);
    }

    public Double getLengthInMm() {
        return getUnderlyingNode().hasProperty(LENGTH_IN_MM) ?
                (Double) getUnderlyingNode().getProperty(LENGTH_IN_MM) : null;
    }

    public Iterable<Relationship> getClassifications() {
        return getUnderlyingNode().getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
    }

    public void classifyAs(TaxonNode taxon) {
        createRelationshipTo(taxon, RelTypes.CLASSIFIED_AS);
    }

    public void setLengthInMm(Double lengthInMm) {
        if (lengthInMm != null) {
            setPropertyWithTx(LENGTH_IN_MM, lengthInMm);
        }
    }

    public void setVolumeInMilliLiter(Double volumeInMm3) {
        setPropertyWithTx(VOLUME_IN_ML, volumeInMm3);
    }

    public void setStomachVolumeInMilliLiter(Double volumeInMilliLiter) {
        setPropertyWithTx(STOMACH_VOLUME_ML, volumeInMilliLiter);
    }

    public void interactsWith(Specimen target, InteractType type, LocationNode centroid) {
        caughtIn(centroid);
        target.caughtIn(centroid);
        interactsWith(target, type);
    }

    public void interactsWith(Specimen recipientSpecimen, InteractType relType) {
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            createInteraction(this, recipientSpecimen, relType);
            tx.success();
        } finally {
            tx.finish();
        }
    }

    protected static void createInteraction(NodeBacked donorSpecimen, NodeBacked recipientSpecimen, InteractType relType) {
        donorSpecimen.createRelationshipToNoTx(recipientSpecimen, relType);
        Relationship inverseRel = recipientSpecimen.createRelationshipToNoTx(donorSpecimen, InteractType.inverseOf(relType));
        if (inverseRel != null) {
            inverseRel.setProperty(PropertyAndValueDictionary.INVERTED, PropertyAndValueDictionary.TRUE);
        }
    }

    public String getOriginalTaxonDescription() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(RelTypes.ORIGINALLY_DESCRIBED_AS, Direction.OUTGOING);
        return singleRelationship == null ? null : new TaxonNode(singleRelationship.getEndNode()).getName();
    }

    public void setOriginalTaxonDescription(Taxon taxon) {
        Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
                try {
                    TaxonNode taxonNode = new TaxonNode(getUnderlyingNode().getGraphDatabase().createNode(), taxon.getName());
                    TaxonUtil.copy(taxon, taxonNode);
                    createRelationshipTo(taxonNode, RelTypes.ORIGINALLY_DESCRIBED_AS);
                    transaction.success();
                } finally {
                    transaction.finish();
                }
    }

    public void setLifeStage(List<Term> lifeStages) {
        if (lifeStages != null && lifeStages.size() > 0) {
            setLifeStage(lifeStages.get(0));
        }
    }

    public void setLifeStage(Term lifeStage) {
        setPropertyWithTx(Specimen.LIFE_STAGE_LABEL, lifeStage.getName());
        setPropertyWithTx(Specimen.LIFE_STAGE_ID, lifeStage.getId());
    }

    public void setPhysiologicalState(Term physiologicalState) {
        setPropertyWithTx(Specimen.PHYSIOLOGICAL_STATE_LABEL, physiologicalState.getName());
        setPropertyWithTx(Specimen.PHYSIOLOGICAL_STATE_ID, physiologicalState.getId());
    }

    public void setBodyPart(List<Term> bodyParts) {
        if (bodyParts != null && bodyParts.size() > 0) {
            setBodyPart(bodyParts.get(0));
        }
    }

    public void setBodyPart(Term bodyPart) {
        setPropertyWithTx(Specimen.BODY_PART_LABEL, bodyPart.getName());
        setPropertyWithTx(Specimen.BODY_PART_ID, bodyPart.getId());
    }

    public void setBasisOfRecord(Term basisOfRecord) {
        setPropertyWithTx(Specimen.BASIS_OF_RECORD_LABEL, basisOfRecord.getName());
        setPropertyWithTx(Specimen.BASIS_OF_RECORD_ID, basisOfRecord.getId());
    }

    public Term getBasisOfRecord() {
        return new Term(getPropertyStringValueOrNull(BASIS_OF_RECORD_ID), getPropertyStringValueOrNull(BASIS_OF_RECORD_LABEL));
    }

    public void setFrequencyOfOccurrence(Double frequencyOfOccurrence) {
        setPropertyWithTx(Specimen.FREQUENCY_OF_OCCURRENCE, frequencyOfOccurrence);
    }

    public void setTotalCount(Integer totalCount) {
        setPropertyWithTx(Specimen.TOTAL_COUNT, totalCount);
    }


    public void setTotalVolumeInMl(Double totalVolumeInMl) {
        setPropertyWithTx(Specimen.TOTAL_VOLUME_IN_ML, totalVolumeInMl);
    }

    public Term getLifeStage() {
        return new Term(getPropertyStringValueOrNull(LIFE_STAGE_ID), getPropertyStringValueOrNull(LIFE_STAGE_LABEL));
    }

    public Term getBodyPart() {
        return new Term(getPropertyStringValueOrNull(BODY_PART_ID), getPropertyStringValueOrNull(BODY_PART_LABEL));
    }

}