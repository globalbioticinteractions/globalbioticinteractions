package org.trophic.graph.domain;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.annotation.Indexed;

import java.util.Date;
import java.util.Set;

/**
 * @author mh
 * @since 12.03.11
 */
@NodeEntity
public class Species {
    @Indexed
    String id;
    
    String scientificName;

    public Species(String id, String scientificName) {
        this.id = id;
        this.scientificName = scientificName;
    }

    public Species() {
    }

    public String getId() {
        return id;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", scientificName, id);
    }

}
