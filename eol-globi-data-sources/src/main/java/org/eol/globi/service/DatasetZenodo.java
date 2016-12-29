package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;

public class DatasetZenodo extends DatasetImpl {
    public DatasetZenodo(String namespace, URI zenodoGitHubArchives) {
        super(namespace, zenodoGitHubArchives);
    }

    @Override
    public String getDOI() {
        String doi = getOrDefault("doi", "");
        if (StringUtils.isBlank(doi)) {
            String recordZenodo = StringUtils.replace(getArchiveURI().toString(), "https://zenodo.org/record/", "");
            String[] split = recordZenodo.split("/");
            if (split.length > 0) {
                doi = "https://doi.org/10.5281/zenodo." + split[0];
            }
        }
        return doi;
    }

}
