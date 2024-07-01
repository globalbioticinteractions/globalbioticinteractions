package org.globalbioticinteractions.dataset;

import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetRegistryProxyIT {

    @Test
    public void zenodoGitHubTest() throws DatasetRegistryException {
        ResourceServiceHTTP resourceService = new ResourceServiceHTTP(new InputStreamFactoryNoop());
        DatasetRegistryProxy proxy = new DatasetRegistryProxy(Arrays.asList(
                new DatasetRegistryZenodo(resourceService),
                new DatasetRegistryGitHubArchive(resourceService))
        );

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), CoreMatchers.containsString("zenodo.org"));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), CoreMatchers.containsString("zenodo.org"));
    }

    @Test
    public void gitHubOnlyTest() throws DatasetRegistryException {
        DatasetRegistryProxy proxy = new DatasetRegistryProxy(
                Collections.singletonList(
                        new DatasetRegistryGitHubArchive(new ResourceServiceHTTP(inStream -> inStream))
                )
        );

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), not(CoreMatchers.containsString("zenodo.org")));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), CoreMatchers.containsString("github.com"));
    }

}