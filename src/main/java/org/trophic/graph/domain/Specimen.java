package org.trophic.graph.domain;

import java.util.Set;

public class Specimen extends NodeBacked<Specimen> {
    String id;

    private Set<Taxon> classifications;

    private Set<Specimen> stomachContents;

    private Location sampleLocation;

    private Season season;

    private Double lengthInMm;

    public Specimen(String id) {
        this.id = id;
    }

    public Specimen() {
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("[%s]", id);
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

    public void ate(Specimen specimen) {
        this.stomachContents.add(specimen);
    }

    public void caughtIn(Location sampleLocation) {
        this.sampleLocation = sampleLocation;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public void caughtDuring(Season season) {
        this.season = season;
    }

    public Double getLengthInMm() {
        return lengthInMm;
    }

    public void setLengthInMm(Double lengthInMm) {
        this.lengthInMm = lengthInMm;
    }


    public Set<Taxon> getClassifications() {
        return classifications;
    }

    public void setClassifications(Set<Taxon> classifications) {
        this.classifications = classifications;
    }

    public void classifyAs(Taxon taxon) {
        this.classifications.add(taxon);
    }
}
