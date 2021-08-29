package org.eol.globi.domain;

import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;

public class SpecimenNode extends NodeBacked implements Specimen {

    public SpecimenNode(Node node) {
        super(node);
    }

    public SpecimenNode(Node node, Double lengthInMm) {
        this(node);
        getUnderlyingNode().setProperty(PropertyAndValueDictionary.TYPE, SpecimenNode.class.getSimpleName());
        if (null != lengthInMm) {
            getUnderlyingNode().setProperty(SpecimenConstant.LENGTH_IN_MM, lengthInMm);
        }
    }

    private static void createInteraction(NodeBacked source, InteractType relType, NodeBacked target) {
        boolean isFlipped = relType.sourceRole == InteractType.InteractionRole.OBJECT;

        final Relationship interactRel = source.createRelationshipToNoTx(target, relType);
        enrichWithInteractProps(relType, interactRel, isFlipped);

        final InteractType inverseRelType = InteractType.inverseOf(relType);
        Relationship inverseInteractRel = target.createRelationshipToNoTx(source, inverseRelType);
        enrichWithInteractProps(inverseRelType, inverseInteractRel, !isFlipped);
    }

    public static void enrichWithInteractProps(InteractType interactType, Relationship interactRel, boolean inverted) {
        if (interactRel != null && interactType != null) {
            interactRel.setProperty(PropertyAndValueDictionary.LABEL, interactType.getLabel());
            interactRel.setProperty(PropertyAndValueDictionary.IRI, interactType.getIRI());
            if (inverted) {
                interactRel.setProperty(PropertyAndValueDictionary.INVERTED, PropertyAndValueDictionary.TRUE);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("[%s]", getUnderlyingNode().getProperty(PropertyAndValueDictionary.TYPE));
    }

    @Override
    public LocationNode getSampleLocation() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.OUTGOING);
        return singleRelationship == null ? null : new LocationNode(singleRelationship.getEndNode());
    }

    @Override
    public void ate(Specimen specimen) {
        interactsWith(specimen, InteractType.ATE);
    }

    @Override
    public void caughtIn(Location sampleLocation) {
        if ((sampleLocation instanceof LocationNode)) {
            createRelationshipTo(sampleLocation, RelTypes.COLLECTED_AT);
        }
    }

    @Override
    public Season getSeason() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CAUGHT_DURING), Direction.OUTGOING);
        return singleRelationship == null ? null : new SeasonNode(singleRelationship.getEndNode());
    }

    @Override
    public void caughtDuring(Season season) {
        createRelationshipTo(season, RelTypes.CAUGHT_DURING);
    }

    @Override
    public Double getLengthInMm() {
        return getUnderlyingNode().hasProperty(SpecimenConstant.LENGTH_IN_MM) ?
                (Double) getUnderlyingNode().getProperty(SpecimenConstant.LENGTH_IN_MM) : null;
    }

    @Override
    public void classifyAs(Taxon taxon) {
        createRelationshipTo(taxon, RelTypes.CLASSIFIED_AS);
    }

    @Override
    public void setLengthInMm(Double lengthInMm) {
        if (lengthInMm != null) {
            this.setProperty(SpecimenConstant.LENGTH_IN_MM, lengthInMm);
        }
    }

    @Override
    public void setVolumeInMilliLiter(Double volumeInMm3) {
        this.setProperty(SpecimenConstant.VOLUME_IN_ML, volumeInMm3);
    }

    @Override
    public void setStomachVolumeInMilliLiter(Double volumeInMilliLiter) {
        this.setProperty(SpecimenConstant.STOMACH_VOLUME_ML, volumeInMilliLiter);
    }

    @Override
    public void interactsWith(Specimen target, InteractType type, Location location) {
        caughtIn(location);
        target.caughtIn(location);
        interactsWith(target, type);
    }

    @Override
    public void interactsWith(Specimen recipientSpecimen, InteractType relType) {
        if (recipientSpecimen instanceof NodeBacked) {
            createInteraction(this, relType, (NodeBacked) recipientSpecimen);
        }
    }

    @Override
    public void setOriginalTaxonDescription(Taxon taxon) {
        TaxonNode taxonNode = new TaxonNode(getUnderlyingNode().getGraphDatabase().createNode(), taxon.getName());
        TaxonUtil.copy(taxon, taxonNode);
        createRelationshipTo(taxonNode, RelTypes.ORIGINALLY_DESCRIBED_AS);
    }

    @Override
    public void setLifeStage(List<Term> lifeStages) {
        if (lifeStages != null && lifeStages.size() > 0) {
            setLifeStage(lifeStages.get(0));
        }
    }

    @Override
    public void setLifeStage(Term lifeStage) {
        if (lifeStage != null) {
            this.setProperty(SpecimenConstant.LIFE_STAGE_LABEL, lifeStage.getName());
            this.setProperty(SpecimenConstant.LIFE_STAGE_ID, lifeStage.getId());
        }
    }

    @Override
    public void setPhysiologicalState(Term physiologicalState) {
        if (physiologicalState != null) {
            this.setProperty(SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL, physiologicalState.getName());
            this.setProperty(SpecimenConstant.PHYSIOLOGICAL_STATE_ID, physiologicalState.getId());
        }
    }

    @Override
    public void setBodyPart(List<Term> bodyParts) {
        if (bodyParts != null && bodyParts.size() > 0) {
            setBodyPart(bodyParts.get(0));
        }
    }

    @Override
    public void setBodyPart(Term bodyPart) {
        if (bodyPart != null) {
            this.setProperty(SpecimenConstant.BODY_PART_LABEL, bodyPart.getName());
            this.setProperty(SpecimenConstant.BODY_PART_ID, bodyPart.getId());
        }
    }

    @Override
    public void setBasisOfRecord(Term basisOfRecord) {
        if (basisOfRecord != null) {
            this.setProperty(SpecimenConstant.BASIS_OF_RECORD_LABEL, basisOfRecord.getName());
            this.setProperty(SpecimenConstant.BASIS_OF_RECORD_ID, basisOfRecord.getId());
        }
    }

    @Override
    public Term getBasisOfRecord() {
        return new TermImpl(getPropertyStringValueOrNull(SpecimenConstant.BASIS_OF_RECORD_ID), getPropertyStringValueOrNull(SpecimenConstant.BASIS_OF_RECORD_LABEL));
    }

    @Override
    public void setFrequencyOfOccurrence(Double frequencyOfOccurrence) {
        this.setProperty(
                SpecimenConstant.FREQUENCY_OF_OCCURRENCE,
                frequencyOfOccurrence
        );
    }

    @Override
    public void setTotalCount(Integer totalCount) {
        setProperty(SpecimenConstant.TOTAL_COUNT, totalCount);
    }


    @Override
    public void setTotalVolumeInMl(Double totalVolumeInMl) {
        setProperty(
                SpecimenConstant.TOTAL_VOLUME_IN_ML,
                totalVolumeInMl);
    }

    @Override
    public Term getLifeStage() {
        return new TermImpl(
                getPropertyStringValueOrNull(SpecimenConstant.LIFE_STAGE_ID),
                getPropertyStringValueOrNull(SpecimenConstant.LIFE_STAGE_LABEL));
    }

    @Override
    public Term getBodyPart() {
        return new TermImpl(
                getPropertyStringValueOrNull(SpecimenConstant.BODY_PART_ID),
                getPropertyStringValueOrNull(SpecimenConstant.BODY_PART_LABEL));
    }

    @Override
    public Term getSex() {
        return new TermImpl(getPropertyStringValueOrNull(SpecimenConstant.SEX_ID), getPropertyStringValueOrNull(SpecimenConstant.SEX_LABEL));
    }

    @Override
    public void setSex(Term term) {
        if (term != null) {
            setPropertyIfNotNull(SpecimenConstant.SEX_LABEL, term.getName());
            setPropertyIfNotNull(SpecimenConstant.SEX_ID, term.getId());
        }
    }


    @Override
    public void setProperty(String name, String value) {
        setPropertyIfNotNull(name, value);
    }

    @Override
    public void setProperty(String name, Double value) {
        setPropertyIfNotNull(name, value);
    }

    @Override
    public String getProperty(String name) {
        return getPropertyValueOrNull(name);
    }

}