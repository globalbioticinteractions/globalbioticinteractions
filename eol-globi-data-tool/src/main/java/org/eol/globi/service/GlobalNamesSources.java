package org.eol.globi.service;

import org.eol.globi.domain.TaxonomyProvider;

public enum GlobalNamesSources {
    GBIF(11, TaxonomyProvider.GBIF),
    NCBI(4, TaxonomyProvider.NCBI),
    IRMNG(8, TaxonomyProvider.ITIS),
    IF(5, TaxonomyProvider.INDEX_FUNGORUM),
    ITIS(3, TaxonomyProvider.ITIS),
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
