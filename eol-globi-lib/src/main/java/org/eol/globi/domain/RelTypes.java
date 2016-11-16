package org.eol.globi.domain;

public enum RelTypes implements RelType {
    CLASSIFIED_AS,
    COLLECTED_AT,
    CAUGHT_DURING,
    ORIGINALLY_DESCRIBED_AS,
    HAS_ENVIRONMENT,
    HAS_LOG_MESSAGE,
    IN_ECOREGION,
    SAME_AS(NameType.SAME_AS),
    SIMILAR_TO(NameType.SIMILAR_TO),
    SYNONYM_OF(NameType.SYNONYM_OF),
    COLLECTED;


    private final NameType type;

    RelTypes(NameType type) {
        this.type = type;
    }

    RelTypes() {
        this(NameType.NONE);
    }

    public static RelTypes forType(NameType type) {
        for (RelTypes relType : values()) {
            if (relType.type.equals(type)) {
                return relType;
            }
        }
        throw new IllegalArgumentException("unsupport nameType [" + type + "]");
    }
}
