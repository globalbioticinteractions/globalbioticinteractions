package org.globalbioticinteractions.dataset;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class DatasetRegistryZenodoIT {

    @Test
    public void zenodoDataFeed() throws DatasetRegistryException, IOException {
        String feed = DatasetRegistryZenodo.getFeed();
        assertThat(feed, containsString("<?xml version"));
    }

    @Test
    public void unlikelyMatch() throws DatasetRegistryException, IOException {
        DatasetRegistryZenodo datasetRegistryZenodo = new DatasetRegistryZenodo(in -> in);
        Dataset thisshouldnotexist = datasetRegistryZenodo.datasetFor("thisshouldnotexist");
        assertNull(thisshouldnotexist);
    }

    @Test
    public void extractGitHubReposArchives() throws DatasetRegistryException {
        URI uri = new DatasetRegistryZenodo(inStream -> inStream).datasetFor("globalbioticinteractions/template-dataset").getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/1436853/files/globalbioticinteractions/template-dataset-0.0.3.zip"));
    }

    @Test
    public void extractGitHubReposArchives2() throws DatasetRegistryException {
        URI uri = new DatasetRegistryZenodo(inStream -> inStream).datasetFor("millerse/Lara-C.-2006").getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/258208/files/millerse/Lara-C.-2006-v1.0.zip"));
    }

    @Test
    public void extractGitHubReposArchives3() throws DatasetRegistryException {
        URI uri = new DatasetRegistryZenodo(inStream -> inStream).datasetFor("millerse/Lichenous").getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/545807/files/millerse/Lichenous-v2.0.0.zip"));
    }

}