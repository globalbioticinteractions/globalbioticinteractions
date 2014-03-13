package org.eol.globi.service;

import org.eol.globi.domain.TaxonomyProvider;

public enum GlobalNamesSources {
    ITIS(3, TaxonomyProvider.ITIS),
    NCBI(4, TaxonomyProvider.NCBI);

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
