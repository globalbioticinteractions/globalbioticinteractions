package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.doi.DOI;

import java.net.URI;

public class DatasetZenodo extends DatasetImpl {
    public DatasetZenodo(String namespace, URI zenodoGitHubArchives, InputStreamFactory inputStreamFactory) {
        super(namespace, zenodoGitHubArchives, inputStreamFactory);
    }

    @Override
    public DOI getDOI() {
        return CitationUtil.getDOI(this);
    }


}
