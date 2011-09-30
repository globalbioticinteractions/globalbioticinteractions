package org.trophic.graph.domain;

import static org.springframework.data.neo4j.core.Direction.INCOMING;

import java.util.Date;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;

@NodeEntity
public class Specimen {
    @Indexed
    String id;
    
    @RelatedTo(elementClass = Species.class, type = "CLASSIFIED_AS", direction = INCOMING)
    Species species;
    
    @RelatedTo(elementClass = Specimen.class, type = "ATE")
    private Set<Specimen> stomachContents;
    
    @RelatedTo(elementClass = Location.class, type = "COLLECTED_AT")
    private Location sampleLocation;
    
    private Date sampleDate;

    public Specimen(String id) {
        this.id = id;
    }

    public Specimen() {
    }

    public String getId() {
        return id;
    }

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species species) {
        this.species = species;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", species, id);
    }

	public void setStomachContents(Set<Specimen> stomachContents) {
		this.stomachContents = stomachContents;
	}

	public Set<Specimen> getStomachContents() {
		return stomachContents;
	}

	public void setSampleLocation(Location sampleLocation) {
		this.sampleLocation = sampleLocation;
	}

	public Location getSampleLocation() {
		return sampleLocation;
	}

	public void setSampleDate(Date sampleDate) {
		this.sampleDate = sampleDate;
	}

	public Date getSampleDate() {
		return sampleDate;
	}
	
    public void ate(Specimen specimen) {
        this.stomachContents.add(specimen);
    }

	public void collectedIn(Location sampleLocation) {
		this.sampleLocation = sampleLocation;
	}

	public void collectedAsPartOf(Study study) {
		study.getSpecimens().add(this);
	}

	public void collectedAt(Date sampleDate) {
		this.sampleDate = sampleDate;
	}


}
