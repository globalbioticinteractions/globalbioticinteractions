package org.eol.globi.domain;

import java.net.URI;

public class PropertyAndValueDictionary {
    public static final String NO_MATCH = "no:match";
    public static final String NAME = "name";
    public static final String EXTERNAL_ID = "externalId";
    public static final String PATH = "path";
    public static final String PATH_NAMES = "pathNames";
    public static final String PATH_IDS = "pathIds";
    public static final String COMMON_NAMES = "commonNames";
    public static final String THUMBNAIL_URL = "thumbnailUrl";
    public static final String EXTERNAL_URL = "externalUrl";
    public static final String INVERTED = "inverted";
    public static final String RO_NAMESPACE = "http://purl.obolibrary.org/obo/RO_";
    public static final String STATUS_ID = "statusId";
    public static final String STATUS_LABEL = "statusLabel";
    public static final String DCTERMS_BIBLIOGRAPHIC_CITATION = "dcterms:bibliographicCitation";
    public static final String OCCURRENCE_ID = "occurrenceId";
    public static final String CATALOG_NUMBER = "catalogNumber";
    public static final String COLLECTION_CODE = "collectionCode";
    public static final String COLLECTION_ID = "collectionId";
    public static final String INSTITUTION_CODE = "institutionCode";
    public static final String MIME_TYPE_DWCA = "application/dwca";
    protected final static String TYPE = "type";
    public static final String RANK = "rank";
    public static final String NO_NAME = "no name";
    public static final java.lang.String EXTERNAL_IDS = "externalIds";
    public static final String TRUE = "true";
    public static final String NUMBER_OF_INTERACTIONS = "nInteractions";
    public static final String NUMBER_OF_DISTINCT_TAXA = "nTaxa";
    public static final java.lang.String NUMBER_OF_SOURCES = "nSources";
    public static final java.lang.String NUMBER_OF_DATASETS = "nDatasets";
    public static final java.lang.String NUMBER_OF_STUDIES = "nStudies";
    public static final String COLLECTION = "collection";
    public static final String LABEL = "label";
    public static final String IRI = "iri";
    public static final String NUMBER_OF_DISTINCT_TAXA_NO_MATCH = "nTaxaNoMatch";
    public static final String NAME_IDS = "nameIds";
    public static final String NAME_SOURCE = "nameSource";
    public static final String NAME_SOURCE_URL = "nameSourceUrl";
    public static final String NAME_SOURCE_ACCESSED_AT = "nameSourceAccessedAt";
    public static final String NAME_MATCH_TYPE = "nameMatchType";

    // see also http://purl.obolibrary.org/obo/SEPIO_0000008 from https://github.com/monarch-initiative/SEPIO-ontology
    public static final String REFUTES = "https://en.wiktionary.org/wiki/refute";
    // see also http://purl.obolibrary.org/obo/SEPIO_0000007 from https://github.com/monarch-initiative/SEPIO-ontology
    public static final String SUPPORTS = "https://en.wiktionary.org/wiki/support";

    public static final URI SPARQL_ENDPOINT_OPEN_BIODIV = URI.create("http://graph.openbiodiv.net/repositories/OpenBiodiv2020");


}
