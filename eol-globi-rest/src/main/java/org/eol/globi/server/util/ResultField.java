package org.eol.globi.server.util;

public enum ResultField {
    LATITUDE("latitude", "Same as DwC decimalLatitude (http://rs.tdwg.org/dwc/terms/decimalLatitude)."),
    LONGITUDE("longitude", "Same as DwC decimalLongitude (http://rs.tdwg.org/dwc/terms/decimalLongitude)."),
    ALTITUDE("altitude", "Similar to elevation and expressed in meters. Negative altitude describes depth related to sea level."),
    FOOTPRINT_WKT("footprintWKT", "Same as DwC footprintWKT (http://rs.tdwg.org/dwc/terms/footprintWKT)."),
    LOCALITY("locality", "Same as DwC locality (http://rs.tdwg.org/dwc/terms/locality)."),
    EVENT_DATE("event_date", "Same as DwC eventDate (http://rs.tdwg.org/dwc/terms/eventDate)."),
    STUDY_TITLE("study_title", "Internal non-unique identifier used to refer to a specific claim, or group of claims."),
    INTERACTION_TYPE("interaction_type", "A human readable description of the interaction . This is the verb (or predicate) that connects the source (or subject) with their target (or object). Also known as \"interaction_type_name\" (e.g., \"eats\"). See also the machine readable URI counterpart  (interaction_type_id, e.g., \"http://purl.obolibrary.org/obo/RO_0002470\")."),

    TARGET_TAXON_NAME("target_taxon_name", "Most granular taxonomic name of the object of the interaction (subject -> source, object -> target). Same as DwC scientificName (http://rs.tdwg.org/dwc/terms/scientificName) may not include authorship. If the subject or object not an organism, this may describe an abiotic interaction partner (e.g., \"vehicle\", \"pack of cigarettes\")."),
    SOURCE_TAXON_NAME("source_taxon_name", "Most granular taxonomic name of the subject of the interaction (subject -> source, object -> target). Same as DwC scientificName (http://rs.tdwg.org/dwc/terms/scientificName) may not include authorship. If the subject or object not an organism, this may describe an abiotic interaction partner (e.g., \"vehicle\", \"pack of cigarettes\")."),

    TARGET_TAXON_COMMON_NAMES("target_taxon_common_names", "Common (or vernacular) name describing the interaction target (or object). Same as DwC vernacularName (http://rs.tdwg.org/dwc/terms/vernacularName). Maybe a list delimited by pipes \"|\" where names may be suffixed by their language code (e.g., \"humans @en | mens @nl | mensch @de\")."),
    SOURCE_TAXON_COMMON_NAMES("source_taxon_common_names", "Common (or vernacular) name describing the interaction source (or subject). Same as DwC vernacularName (http://rs.tdwg.org/dwc/terms/vernacularName). Maybe a list delimited by pipes \"|\" where names may be suffixed by their language code (e.g., \"humans @en | mens @nl | mensch @de\")."),

    TARGET_TAXON_EXTERNAL_ID("target_taxon_external_id", "a single existing identifier associated with the taxonomic name of the target. Ids are prefixed using the schemes documented in https://api.globalbioticinteractions.org/prefixes (e.g., NCBI:9606 refers to NCBI's Taxon ID relating to Homo sapiens). Same as DwC taxonID (http://rs.tdwg.org/dwc/terms/taxonID)."),
    SOURCE_TAXON_EXTERNAL_ID("source_taxon_external_id", "a single existing identifier associated with the taxonomic name of the source. Ids are prefixed using the schemes documented in https://api.globalbioticinteractions.org/prefixes (e.g., NCBI:9606 refers to NCBI's Taxon ID relating to Homo sapiens). Same as DwC taxonID (http://rs.tdwg.org/dwc/terms/taxonID)."),

    TARGET_TAXON_PATH("target_taxon_path", "a pipe limited list of name hierarchy related to the target taxon name. Only includes elements from a single taxonomic resource. Same as DwC HigherTaxon (http://rs.tdwg.org/dwc/terms/HigherTaxon) E.g., \\\"cellular organisms | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens\" for the name hierarchy related to NCBI:9606 Homo sapiens . Note that for abiotic names, this may include a hierarchy defined by some ontology. E.g. plastic as defined by ENVO:01000404 may have hierarchy including (\"environmental material | anthropogenic environmental material | ... | plastic\")."),
    SOURCE_TAXON_PATH("source_taxon_path", "a pipe limited list of name hierarchy related to the source taxon name. Only includes elements from a single taxonomic resource. Same as DwC HigherTaxon (http://rs.tdwg.org/dwc/terms/HigherTaxon) E.g., \\\"cellular organisms | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens\" for the name hierarchy related to NCBI:9606 Homo sapiens."),

    TARGET_TAXON_PATH_RANKS("target_taxon_path_ranks", "pipe delimited list of taxonomic ranks of the target taxon hierarchy. E.g., NCBI's Homo sapiens NCBI:9606 (\"| superkingdom | clade | kingdom | clade | clade | clade | phylum | subphylum | clade | clade | clade | clade | superclass | clade | clade | clade | class | clade | clade | clade | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | species\") where empty values represent undefined or unknown ranks."),
    SOURCE_TAXON_PATH_RANKS("source_taxon_path_ranks", "pipe delimited list of taxonomic ranks of the source taxon hierarchy. E.g., NCBI's Homo sapiens NCBI:9606 (\"| superkingdom | clade | kingdom | clade | clade | clade | phylum | subphylum | clade | clade | clade | clade | superclass | clade | clade | clade | class | clade | clade | clade | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | species\") where empty values represent undefined or unknown ranks."),

    TARGET_TAXON_PATH_IDS("target_taxon_path_ids", "pipe delimited list of taxonomic identifiers of the target taxon hierarchy. E.g., NCBI's Homo sapiens NCBI:9606 \"NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606\" ."),
    SOURCE_TAXON_PATH_IDS("source_taxon_path_ids", "pipe delimited list of taxonomic identifiers of the source taxon hierarchy. E.g., NCBI's Homo sapiens NCBI:9606 \"NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606\" ."),

    TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE(
            "target_specimen_frequency_of_occurrence",
            "Frequency at which the interaction between source and target was observed."),
    TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE_PERCENT(
            "target_specimen_frequency_of_occurrence_percent",
            "Relative frequency of occurrence of a specific interaction between souce and target relating to interactions with other targets. E.g., this bee visited 10% flowers of X and 90% flower of Y. coupled with a study or group of observation events."),
    TARGET_SPECIMEN_TOTAL_VOLUME_ML("target_specimen_total_volume_ml", "Total volume of the stomach of the source (or subject)."),
    TARGET_SPECIMEN_TOTAL_VOLUME_PERCENT("target_specimen_total_volume_ml_percent", "Percentage of the stomach volume occupied by the specimen of classified by the target. (e.g., a stomach of an Atlantic cod contained 25% crabs. (source: Atlantic cod, target: crab)."),
    TARGET_SPECIMEN_TOTAL_COUNT("target_specimen_total_count", "Absolute number of observations that documented the interaction of between source/target individuals (e.g., this bee visited that flower 14 times)."),
    TARGET_SPECIMEN_TOTAL_COUNT_PERCENT("target_specimen_total_count_percent", "Relative number of observations that documented the interaction between source/target individuals (e.g., this bee visited that flower 40% of the time)."),

    TARGET_SPECIMEN_ID("tmp_and_unique_target_specimen_id", "GloBI internal identifier for target (or object) record (e.g., a record describing a specimen acted as the target (or subject) of the interaction)."),
    SOURCE_SPECIMEN_ID("tmp_and_unique_source_specimen_id", "GloBI internal identifier for source (or subject) record (e.g., a record describing a specimen acted as the target (or subject) of the interaction)."),

    TARGET_SPECIMEN_OCCURRENCE_ID("target_specimen_occurrence_id", "External identifier for target (or object) record (e.g., http://arctos.database.museum/guid/DMNS:Mamm:18530). Similar to http://rs.tdwg.org/dwc/terms/occurrenceID ."),
    SOURCE_SPECIMEN_OCCURRENCE_ID("source_specimen_occurrence_id", "External identifier for source (or subject) record (e.g., http://arctos.database.museum/guid/DMNS:Mamm:18530). Similar to http://rs.tdwg.org/dwc/terms/occurrenceID . "),
    TARGET_SPECIMEN_CATALOG_NUMBER("target_specimen_catalog_number", "External catalogue number for target (or object) record (e.g., \"DMNS:Mamm:18703\"). Similar to http://rs.tdwg.org/dwc/terms/catalogNumber ." ),
    SOURCE_SPECIMEN_CATALOG_NUMBER("source_specimen_catalog_number", "External catalogue number for source (or subject) record (e.g., \"DMNS:Mamm:18703\"). Similar to http://rs.tdwg.org/dwc/terms/catalogNumber ."),
    TARGET_SPECIMEN_INSTITUTION_CODE("target_specimen_institution_code", "External institution code for target (or object) record (e.g., \"DMNS\"). Similar to http://rs.tdwg.org/dwc/terms/institutionCode."),
    SOURCE_SPECIMEN_INSTITUTION_CODE("source_specimen_institution_code", "External institution code for source (or subject) record (e.g., \"DMNS\"). Similar to http://rs.tdwg.org/dwc/terms/institutionCode."),
    TARGET_SPECIMEN_COLLECTION_CODE("target_specimen_collection_code", "External collection code for target (or object) record (e.g., \"Mamm\"). Similar to http://rs.tdwg.org/dwc/terms/institutionCode."),
    SOURCE_SPECIMEN_COLLECTION_CODE("source_specimen_collection_code", "External collection code for source (or subject) record (e.g., \"Mamm\"). Similar to http://rs.tdwg.org/dwc/terms/institutionCode."),

    TARGET_SPECIMEN_PHYSIOLOGICAL_STATE("target_specimen_physiological_state"),
    TARGET_SPECIMEN_PHYSIOLOGICAL_STATE_ID("target_specimen_physiological_state_id"),
    SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE( "source_specimen_physiological_state"),
    SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE_ID( "source_specimen_physiological_state_id"),

    TARGET_SPECIMEN_BODY_PART("target_specimen_body_part"),
    TARGET_SPECIMEN_BODY_PART_ID("target_specimen_body_part_id"),
    SOURCE_SPECIMEN_BODY_PART("source_specimen_body_part"),
    SOURCE_SPECIMEN_BODY_PART_ID("source_specimen_body_part_id"),

    SOURCE_SPECIMEN_LIFE_STAGE("source_specimen_life_stage"),
    SOURCE_SPECIMEN_LIFE_STAGE_ID("source_specimen_life_stage_id"),
    TARGET_SPECIMEN_LIFE_STAGE("target_specimen_life_stage"),
    TARGET_SPECIMEN_LIFE_STAGE_ID("target_specimen_life_stage_id"),

    SOURCE_SPECIMEN_SEX("source_specimen_sex"),
    SOURCE_SPECIMEN_SEX_ID("source_specimen_sex_id"),
    TARGET_SPECIMEN_SEX("target_specimen_sex"),
    TARGET_SPECIMEN_SEX_ID("target_specimen_sex_id"),

    SOURCE_SPECIMEN_BASIS_OF_RECORD("source_specimen_basis_of_record"),
    TARGET_SPECIMEN_BASIS_OF_RECORD("target_specimen_basis_of_record"),

    TAXON_NAME("taxon_name"),
    TAXON_COMMON_NAMES("taxon_common_names"),
    TAXON_EXTERNAL_ID("taxon_external_id"),

    TAXON_EXTERNAL_URL("taxon_external_url"),
    TAXON_PATH("taxon_path"),
    TAXON_PATH_IDS("taxon_path_ids"),
    TAXON_PATH_RANKS("taxon_path_ranks"),
    STUDY_URL("study_url"),
    STUDY_DOI("study_doi"),
    STUDY_CITATION("study_citation"),
    STUDY_SOURCE_CITATION("study_source_citation"),
    STUDY_SOURCE_ID("study_source_id"),
    NUMBER_OF_DISTINCT_TAXA("number_of_distinct_taxa", "only available for /reports/* queries"),
    NUMBER_OF_DISTINCT_TAXA_NO_MATCH("number_of_distinct_taxa_no_match", "only available for /reports/* queries"),
    NUMBER_OF_SOURCES("number_of_sources", "only available for /reports/* queries"),
    NUMBER_OF_STUDIES("number_of_studies", "only available for /reports/* queries"),
    NUMBER_OF_INTERACTIONS("number_of_interactions", "available for /interaction queries by source/target taxon name and/or interactionType only"),
    STUDY_SOURCE_DOI("study_source_doi"),
    STUDY_SOURCE_FORMAT("study_source_format"),
    STUDY_SOURCE_ARCHIVE_URI("study_source_archive_uri"),
    STUDY_SOURCE_LAST_SEEN_AT("study_source_last_seen_at");

    public String getLabel() {
        return label;
    }

    private final String label;
    private final String description;

    ResultField(String label) {
        this(label, "a description of " + label);
    }

    ResultField(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String toString() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
