package org.globalbioticinteractions.dataset;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetRegistryProxyIT {

    @Test
    public void zenodoGitHubTest() throws DatasetRegistryException {
        DatasetRegistryProxy proxy = new DatasetRegistryProxy(Arrays.asList(
                new DatasetRegistryZenodo(inStream -> inStream),
                new DatasetRegistryGitHubArchive(inStream -> inStream))
        );

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), CoreMatchers.containsString("zenodo.org"));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), CoreMatchers.containsString("github.com"));
    }

    @Test
    public void gitHubOnlyTest() throws DatasetRegistryException {
        DatasetRegistryProxy proxy = new DatasetRegistryProxy(Collections.singletonList(new DatasetRegistryGitHubArchive(inStream -> inStream)));

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), not(CoreMatchers.containsString("zenodo.org")));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), CoreMatchers.containsString("github.com"));
    }

}