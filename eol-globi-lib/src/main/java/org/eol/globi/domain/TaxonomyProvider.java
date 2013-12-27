package org.eol.globi.domain;

public enum TaxonomyProvider {
    ITIS("urn:lsid:itis.gov:itis_tsn:"),
    WORMS("urn:lsid:marinespecies.org:taxname:"),
    NCBI("ncbi:"),
    EOL("EOL:");

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
    public static final String ID_PREFIX_GULFBASE = "BioGoMx:";
    public static final String ID_PREFIX_GAME = "GAME:";
    public static final String ID_PREFIX_HTTP = "http://";
    public static final String ID_PREFIX_USKI = "UKSI:";
    public static final String BIO_INFO = "bioinfo:";

}
