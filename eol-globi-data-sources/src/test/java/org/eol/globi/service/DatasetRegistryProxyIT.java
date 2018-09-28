package org.eol.globi.service;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

public class DatasetRegistryProxyIT {

    @Test
    public void zenodoGitHubTest() throws DatasetFinderException {
        DatasetRegistryProxy proxy = new DatasetRegistryProxy(Arrays.asList(new DatasetRegistryZenodo(), new DatasetRegistryGitHubArchive()));

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), containsString("zenodo.org"));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
    }

    @Test
    public void gitHubOnlyTest() throws DatasetFinderException {
        DatasetRegistryProxy proxy = new DatasetRegistryProxy(Arrays.asList(new DatasetRegistryGitHubArchive()));

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), not(containsString("zenodo.org")));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
    }

}