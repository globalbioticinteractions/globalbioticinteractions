package org.eol.globi.service;

import org.junit.Test;

import java.net.URI;
import java.util.Collection;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class DatasetFinderGitHubIT {

    @Test
    public void discoverDatasetsInGitHub() throws DatasetFinderException {
        Collection<String> urls = new DatasetFinderGitHubArchive().findNamespaces();
        assertThat(urls.size(), is(not(0)));
    }

    @Test
    public void datasetFor() throws DatasetFinderException {
        URI uri = new DatasetFinderGitHubArchive().datasetFor("globalbioticinteractions/template-dataset").getArchiveURI();
        assertThat(uri.toString(), startsWith("https://github.com/globalbioticinteractions/template-dataset/archive/"));
        assertThat(uri.toString(), endsWith(".zip"));
    }

}