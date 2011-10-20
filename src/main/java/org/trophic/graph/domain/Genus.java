package org.trophic.graph.domain;

public class Genus extends Taxon<Genus> {
    Family family;


    public Genus(String name) {
        this.name = name;
    }

    public Genus() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s", name);
    }


    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public void partOf(Family family) {
        this.family = family;
    }

}
