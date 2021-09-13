package org.globalbioticinteractions.dataset;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetRegistryZenodoIT {

    @Test
    public void zenodoDataFeed() throws DatasetRegistryException {
        String feed = DatasetRegistryZenodo.getNextPage(in -> in, null);
        assertThat(feed, containsString("<?xml version"));
    }

    @Test
    public void unlikelyMatch() throws DatasetRegistryException {
        DatasetRegistryZenodo datasetRegistryZenodo = new DatasetRegistryZenodo(in -> in);
        Dataset thisshouldnotexist = datasetRegistryZenodo.datasetFor("thisshouldnotexist");
        assertNull(thisshouldnotexist);
    }

    @Test
    public void extractGitHubReposArchives() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(inStream -> inStream)
                .datasetFor("globalbioticinteractions/template-dataset");

        assertNotNull(dataset);
        URI uri = dataset
                .getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/1436853/files/globalbioticinteractions/template-dataset-0.0.3.zip"));
    }

    @Test
    public void extractGitHubReposArchives2() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(inStream -> inStream)
                .datasetFor("millerse/Lara-C.-2006");

        assertThat(dataset, is(notNullValue()));
        URI uri = dataset
                .getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/258208/files/millerse/Lara-C.-2006-v1.0.zip"));
    }

    @Test
    public void extractGitHubReposArchives3() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(inStream -> inStream)
                .datasetFor("millerse/Lichenous");

        assertThat(dataset, is(notNullValue()));

        URI uri = dataset
                .getArchiveURI();

        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/545807/files/millerse/Lichenous-v2.0.0.zip"));
    }

}