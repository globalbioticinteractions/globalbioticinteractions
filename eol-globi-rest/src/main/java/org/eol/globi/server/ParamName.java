package org.eol.globi.server;

public enum ParamName {

    ACCORDING_TO("accordingTo"),

    INTERACTION_TYPE("interactionType"),

    INCLUDE_OBSERVATIONS("includeObservations"),

    FIELD("field"),
    FIELDS("fields"),

    EXACT_NAME_MATCH_ONLY("exactNameMatchOnly"),

    EXCLUDE_CHILD_TAXA("excludeChildTaxa"),

    SOURCE_TAXON("sourceTaxon"),
    TARGET_TAXON("targetTaxon"),
    TAXON("taxon"),

    BBOX("bbox"),

    TAXON_ID_PREFIX("taxonIdPrefix");

    private String name;

    ParamName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
