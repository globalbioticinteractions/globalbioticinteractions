package org.eol.globi.taxon;

import org.eol.globi.domain.TaxonomyProvider;

public enum GlobalNamesSources {
    GBIF(11, TaxonomyProvider.GBIF),
    NCBI(4, TaxonomyProvider.NCBI),
    IRMNG(8, TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA),
    IF(5, TaxonomyProvider.INDEX_FUNGORUM),
    ITIS(3, TaxonomyProvider.ITIS),
    OTT(179, TaxonomyProvider.OPEN_TREE_OF_LIFE),
    FISHBASE_CACHE(177, TaxonomyProvider.FISHBASE_CACHE),
    INATURALIST(180, TaxonomyProvider.INATURALIST_TAXON),
    WORMS(9, TaxonomyProvider.WORMS);

    private final int id;

    private final TaxonomyProvider provider;

    GlobalNamesSources(int id, TaxonomyProvider provider) {
        this.id = id;
        this.provider = provider;
    }

    public int getId() {
        return id;
    }

    public TaxonomyProvider getProvider() {
        return provider;
    }

}
