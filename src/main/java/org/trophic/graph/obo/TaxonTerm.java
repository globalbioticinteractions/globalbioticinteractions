package org.trophic.graph.obo;

public class TaxonTerm {
    public void setRank(String rank) {
        this.rank = rank;
    }

    private String rank;

    public String getName() {
        return name;
    }

    public String getIsA() {
        return isA;
    }

    public String getId() {
        return id;
    }

    private String name;
    private String isA;
    private String id;

    public void setName(String name) {
        this.name = name;
    }

    public void setIsA(String isA) {
        this.isA = isA;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRank() {
        return this.rank;
    }
}
