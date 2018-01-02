package org.eol.globi.domain;

import java.util.List;

public interface Specimen extends WithId {

    Location getSampleLocation();

    void ate(Specimen specimen);

    void caughtIn(Location sampleLocation);

    Season getSeason();

    void caughtDuring(Season season);

    Double getLengthInMm();

    void classifyAs(Taxon taxon);

    void setLengthInMm(Double lengthInMm);

    void setVolumeInMilliLiter(Double volumeInMm3);

    void setStomachVolumeInMilliLiter(Double volumeInMilliLiter);

    void interactsWith(Specimen target, InteractType type, Location centroid);

    void interactsWith(Specimen recipientSpecimen, InteractType relType);

    void setOriginalTaxonDescription(Taxon taxon);

    void setLifeStage(List<TermImpl> lifeStages);

    void setLifeStage(TermImpl lifeStage);

    void setPhysiologicalState(TermImpl physiologicalState);

    void setBodyPart(List<TermImpl> bodyParts);

    void setBodyPart(TermImpl bodyPart);

    void setBasisOfRecord(TermImpl basisOfRecord);

    TermImpl getBasisOfRecord();

    void setFrequencyOfOccurrence(Double frequencyOfOccurrence);

    void setTotalCount(Integer totalCount);

    void setTotalVolumeInMl(Double totalVolumeInMl);

    TermImpl getLifeStage();

    TermImpl getBodyPart();

    void setProperty(String name, Object value);
}
