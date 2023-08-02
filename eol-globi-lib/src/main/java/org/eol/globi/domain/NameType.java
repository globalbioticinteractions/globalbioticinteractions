package org.eol.globi.domain;

public enum NameType {
    SAME_AS,
    SIMILAR_TO,
    SYNONYM_OF,
    COMMON_NAME_OF,
    HAS_ACCEPTED_NAME, // is a kind of "same as" or "equivalent" to
    HAS_UNCHECKED_NAME, // possibly an accepted name, but not yet verified/checked
    HOMONYM_OF,
    HAS_PARSED_NAME, // type associated with the outcome of a (taxonomic) parsing process
    TRANSLATES_TO, // type assicated with a general purpose name/id translation process (aka name "cleaning", name "scrubbing", or name "mapping")
    OCCURS_IN, // name occurs in some text, publication, or dataset
    NONE
}
