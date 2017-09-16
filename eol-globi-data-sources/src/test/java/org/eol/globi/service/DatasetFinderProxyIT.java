package org.eol.globi.service;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

public class DatasetFinderProxyIT {

    @Test
    public void zenodoGitHubTest() throws DatasetFinderException {
        DatasetFinderProxy proxy = new DatasetFinderProxy(Arrays.asList(new DatasetFinderZenodo(), new DatasetFinderGitHubArchive()));

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), containsString("zenodo.org"));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
    }

    @Test
    public void gitHubOnlyTest() throws DatasetFinderException {
        DatasetFinderProxy proxy = new DatasetFinderProxy(Arrays.asList(new DatasetFinderGitHubArchive()));

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), not(containsString("zenodo.org")));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
    }

}