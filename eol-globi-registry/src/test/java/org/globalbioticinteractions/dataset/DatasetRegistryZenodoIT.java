package org.globalbioticinteractions.dataset;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DatasetRegistryZenodoIT {

    @Ignore("datacite3 no longer supported")
    @Test
    public void zenodoDataFeedDatacite3() throws DatasetRegistryException {
        String metadataPrefix = "oai_datacite3";
        String feed = DatasetRegistryZenodo.getNextPage(
                null,
                new ResourceServiceHTTP(new InputStreamFactoryNoop()),
                metadataPrefix);
        assertThat(feed, containsString("<?xml version"));
        assertThat(feed, containsString("metadataPrefix=\"" + metadataPrefix + "\""));
    }

    @Test
    public void zenodoDataFeedDatacite4() throws DatasetRegistryException {
        String metadataPrefix = "oai_datacite";
        String feed = DatasetRegistryZenodo.getNextPage(
                null,
                new ResourceServiceHTTP(new InputStreamFactoryNoop()),
                metadataPrefix);
        assertThat(feed, containsString("<?xml version"));
        assertThat(feed, containsString("metadataPrefix=\"" + metadataPrefix + "\""));
    }

    @Test
    public void unlikelyMatch() throws DatasetRegistryException {
        DatasetRegistryZenodo datasetRegistryZenodo = new DatasetRegistryZenodo(new ResourceServiceHTTP(new InputStreamFactoryNoop()));
        Dataset thisshouldnotexist = datasetRegistryZenodo.datasetFor("thisshouldnotexist");
        assertNull(thisshouldnotexist);
    }

    @Test
    public void extractGitHubReposArchives() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(new ResourceServiceHTTP(new InputStreamFactoryNoop()))
                .datasetFor("globalbioticinteractions/template-dataset");

        assertNotNull(dataset);
        URI uri = dataset
                .getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(),
                is("https://zenodo.org/records/1436853/files/globalbioticinteractions/template-dataset-0.0.3.zip"));
    }

    @Test
    public void extractGitHubReposArchives2() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(new ResourceServiceHTTP(new InputStreamFactoryNoop()))
                .datasetFor("millerse/Lara-C.-2006");

        assertThat(dataset, is(notNullValue()));
        URI uri = dataset
                .getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/records/258208/files/millerse/Lara-C.-2006-v1.0.zip"));
    }

    @Test
    public void extractGitHubReposArchives3() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(new ResourceServiceHTTP(new InputStreamFactoryNoop()))
                .datasetFor("millerse/Lichenous");

        assertThat(dataset, is(notNullValue()));

        URI uri = dataset
                .getArchiveURI();

        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/records/545807/files/millerse/Lichenous-v2.0.0.zip"));
    }

}