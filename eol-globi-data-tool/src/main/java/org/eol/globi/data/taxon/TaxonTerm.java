package org.eol.globi.data.taxon;

public class TaxonTerm {
    private String rankPath;

    public TaxonTerm(String name, String id) {
        this.name = name;
        this.id = id;

    }

    public TaxonTerm() {

    }

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

    public String getRankPath() {
        return rankPath;
    }

    public void setRankPath(String rankPath) {
        this.rankPath = rankPath;
    }
}
