package org.eol.globi.domain;

import org.neo4j.graphdb.Relationship;

import java.util.List;

public interface Specimen {

    Iterable<Relationship> getStomachContents();

    LocationNode getSampleLocation();

    void ate(Specimen specimen);

    void caughtIn(Location sampleLocation);

    Season getSeason();

    void caughtDuring(Season season);

    Double getLengthInMm();

    Iterable<Relationship> getClassifications();

    void classifyAs(Taxon taxon);

    void setLengthInMm(Double lengthInMm);

    void setVolumeInMilliLiter(Double volumeInMm3);

    void setStomachVolumeInMilliLiter(Double volumeInMilliLiter);

    void interactsWith(Specimen target, InteractType type, Location centroid);

    void interactsWith(Specimen recipientSpecimen, InteractType relType);

    String getOriginalTaxonDescription();

    void setOriginalTaxonDescription(Taxon taxon);

    void setLifeStage(List<Term> lifeStages);

    void setLifeStage(Term lifeStage);

    void setPhysiologicalState(Term physiologicalState);

    void setBodyPart(List<Term> bodyParts);

    void setBodyPart(Term bodyPart);

    void setBasisOfRecord(Term basisOfRecord);

    Term getBasisOfRecord();

    void setFrequencyOfOccurrence(Double frequencyOfOccurrence);

    void setTotalCount(Integer totalCount);

    void setTotalVolumeInMl(Double totalVolumeInMl);

    Term getLifeStage();

    Term getBodyPart();
}
