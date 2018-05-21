package org.eol.globi.domain;

public enum TaxonomyProvider {
    ITIS("ITIS:"),
    NBN("NBN:"),
    WORMS("WORMS:"),
    NCBI("NCBI:"),
    NCBITaxon("NCBITaxon:"),
    EOL("EOL:"),
    GEONAMES("GEONAMES:"),
    WIKIPEDIA("W:"),
    ENVO("ENVO:"),
    GBIF("GBIF:"),
    ATLAS_OF_LIVING_AUSTRALIA("ALATaxon:"),
    AUSTRALIAN_FAUNAL_DIRECTORY("AFD:"),
    BIODIVERSITY_AUSTRALIA("urn:lsid:biodiversity.org.au:apni.taxon:"),
    INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA("IRMNG:"),
    INDEX_FUNGORUM("IF:"),
    OPEN_TREE_OF_LIFE("OTT:"),
    NATIONAL_OCEANOGRAPHIC_DATA_CENTER("NODC:"),
    INATURALIST_TAXON("INAT_TAXON:"),
    WIKIDATA("WD:"),
    FISHBASE_CACHE("FBC:"),
    GULFBASE("BioGoMx:");

    private final String idPrefix;

    TaxonomyProvider(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public static final String ID_PREFIX_EOL = EOL.getIdPrefix();
    public static final String ID_PREFIX_INATURALIST = "INAT:";
    public static final String ID_PREFIX_WORMS = WORMS.getIdPrefix();
    public static final String ID_PREFIX_ITIS = ITIS.getIdPrefix();
    public static final String ID_PREFIX_ENVO = ENVO.getIdPrefix();
    public static final String ID_PREFIX_WIKIPEDIA = WIKIPEDIA.getIdPrefix();
    public static final String ID_PREFIX_GULFBASE = GULFBASE.getIdPrefix();
    public static final String ID_PREFIX_GAME = "GAME:";
    public static final String ID_PREFIX_HTTP = "http://";
    public static final String ID_PREFIX_HTTPS = "https://";
    public static final String ID_PREFIX_DOI = "doi:";
    public static final String ID_PREFIX_GBIF = GBIF.getIdPrefix();
    public static final String ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY = AUSTRALIAN_FAUNAL_DIRECTORY.getIdPrefix();
    public static final String ID_PREFIX_BIODIVERSITY_AUSTRALIA = BIODIVERSITY_AUSTRALIA.getIdPrefix();
    public static final String ID_PREFIX_INDEX_FUNGORUM = INDEX_FUNGORUM.getIdPrefix();
    public static final String ID_PREFIX_NCBI = NCBI.getIdPrefix();
    public static final String ID_PREFIX_NBN = NBN.getIdPrefix();

    public static final String ID_CMECS = "http://cmecscatalog.org/classification/aquaticSetting/";
    public static final String ID_BIO_INFO_REFERENCE = "bioinfo:ref:";
    public static final String BIO_INFO = "bioinfo:";

}
