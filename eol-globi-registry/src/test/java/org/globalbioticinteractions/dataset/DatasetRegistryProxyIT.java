package org.globalbioticinteractions.dataset;

import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetRegistryProxyIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void zenodoGitHubTest() throws DatasetRegistryException, IOException {
        ResourceServiceHTTP resourceService = new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
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
    public void gitHubOnlyTest() throws DatasetRegistryException, IOException {
        DatasetRegistryProxy proxy = new DatasetRegistryProxy(
                Collections.singletonList(
                        new DatasetRegistryGitHubArchive(new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder()))
                )
        );

        Dataset dataset = proxy.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(dataset.getArchiveURI().toString(), not(CoreMatchers.containsString("zenodo.org")));

        dataset = proxy.datasetFor("millerse/Bird-Parasite");
        assertThat(dataset.getArchiveURI().toString(), CoreMatchers.containsString("github.com"));
    }

}