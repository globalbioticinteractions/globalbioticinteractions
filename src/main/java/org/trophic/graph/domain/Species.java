package org.trophic.graph.domain;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.Indexed;

@NodeEntity
public class Species extends Taxon {
    @RelatedTo(elementClass = Genus.class, type = "PART_OF")
    Genus genus;

    public Species(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Species() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public Genus getGenus() {
        return genus;
    }

    public void setGenus(Genus genus) {
        this.genus = genus;
    }

    public void partOf(Genus genus) {
        this.genus = genus;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", name, id);
    }

}
