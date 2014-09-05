package org.eol.globi.domain;

public enum TaxonomyProvider {
    ITIS("urn:lsid:itis.gov:itis_tsn:"),
    WORMS("urn:lsid:marinespecies.org:taxname:"),
    NCBI("NCBI:"),
    EOL("EOL:"),
    GEONAMES("GEO:"),
    WIKIPEDIA("W:"),
    ENVO("ENVO:"),
    GBIF("GBIF:"),
    LIVING_ATLAS_OF_AUSTRALIA("urn:lsid:biodiversity.org.au:afd.taxon:"),
    INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA("IRMNG:"),
    INDEX_FUNGORUM("IF:"),
    OPEN_TREE_OF_LIFE("OTT:");

    private final String idPrefix;

    private TaxonomyProvider(String idPrefix) {
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
    public static final String ID_PREFIX_GULFBASE = "BioGoMx:";
    public static final String ID_PREFIX_GAME = "GAME:";
    public static final String ID_PREFIX_HTTP = "http://";
    public static final String ID_PREFIX_USKI = "UKSI:";
    public static final String ID_PREFIX_GBIF = GBIF.getIdPrefix();
    public static final String ID_PREFIX_LIVING_ATLAS_OF_AUSTRALIA = LIVING_ATLAS_OF_AUSTRALIA.getIdPrefix();

    public static final String ID_CMECS = "CMECS:AQUATIC_SETTING:";
    public static final String BIO_INFO = "bioinfo:";

}
