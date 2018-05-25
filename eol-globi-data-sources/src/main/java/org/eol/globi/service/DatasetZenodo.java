package org.eol.globi.service;

import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.doi.DOI;

import java.net.URI;

public class DatasetZenodo extends DatasetImpl {
    public DatasetZenodo(String namespace, URI zenodoGitHubArchives) {
        super(namespace, zenodoGitHubArchives);
    }

    @Override
    public DOI getDOI() {
        return CitationUtil.getDOI(this);
    }


}
