package org.trophic.graph.domain;


public class Family extends Taxon<Family> {
    public Family(String name) {
        this.name = name;
    }

    public Family() {
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

    public Family persist() {
        return null;
    }
}
