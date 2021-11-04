package org.eol.globi.domain;

public enum NameType {
    SAME_AS,
    SIMILAR_TO,
    SYNONYM_OF,
    COMMON_NAME_OF,
    HAS_ACCEPTED_NAME, // is a kind of "same as" or "equivalent" to
    HOMONYM_OF,
    NONE
}
