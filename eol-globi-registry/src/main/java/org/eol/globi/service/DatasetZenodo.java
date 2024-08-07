package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.globalbioticinteractions.doi.DOI;

import java.io.File;
import java.net.URI;

public class DatasetZenodo extends DatasetWithResourceMapping {
    public DatasetZenodo(String namespace, URI zenodoGitHubArchives, InputStreamFactory inputStreamFactory, File cacheDir) {
        super(namespace, zenodoGitHubArchives, new ResourceServiceLocalAndRemote(inputStreamFactory, cacheDir));
    }

    public DatasetZenodo(String namespace, ResourceService resourceService, URI latestArchive) {
        super(namespace, latestArchive, resourceService);
    }

    @Override
    public DOI getDOI() {
        return CitationUtil.getDOI(this);
    }


}
