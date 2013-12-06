package org.eol.globi.domain;

public enum TaxonomyProvider {
    ITIS,
    WORMS,
    NCBI,
    EOL;

    public static final String ID_PREFIX_EOL = "EOL:";
    public static final String ID_PREFIX_INATURALIST = "INAT:";
    public static final String ID_PREFIX_WORMS = "urn:lsid:marinespecies.org:taxname:";
    public static final String ID_PREFIX_ITIS = "urn:lsid:itis.gov:itis_tsn:";
    public static final String ID_PREFIX_GULFBASE = "BioGoMx:";
    public static final String ID_PREFIX_GAME = "GAME:";
    public static final String ID_PREFIX_USKI = "UKSI:";
}
