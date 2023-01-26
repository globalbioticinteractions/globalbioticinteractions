package org.eol.globi.domain;

public enum NameType {
    SAME_AS,
    SIMILAR_TO,
    SYNONYM_OF,
    COMMON_NAME_OF,
    HAS_ACCEPTED_NAME, // is a kind of "same as" or "equivalent" to
    HAS_UNCHECKED_NAME, // possibly an accepted name, but not yet verified/checked
    HOMONYM_OF,
    OCCURS_IN, // name occurs in some text, publication, or dataset
    NONE
}
