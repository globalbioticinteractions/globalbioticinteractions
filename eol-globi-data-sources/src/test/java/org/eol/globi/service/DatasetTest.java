package org.eol.globi.service;

import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DatasetTest {

    @Test
    public void resourceURL() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("archive.zip");
        File archive = new File(resource.toURI());
        assertThat(archive.exists(), is(true));

        URI parentDir = archive.getParentFile().toURI();

        Dataset dataset = new DatasetImpl("some/namespace", parentDir, inStream -> inStream);
        assertThat(dataset.retrieve(resource.toURI()), is(notNullValue()));
        assertThat(dataset.retrieve(URI.create("archive.zip")), is(notNullValue()));
        assertThat(dataset.retrieve(URI.create("/archive.zip")), is(notNullValue()));
    }

    @Test(expected = IOException.class)
    public void unknownResourceURL() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("archive.zip");
        File archive = new File(resource.toURI());
        assertThat(archive.exists(), is(true));

        URI parentDir = archive.getParentFile().toURI();

        Dataset dataset = new DatasetImpl("some/namespace", parentDir, inStream -> inStream);
        assertThat(dataset.retrieve(URI.create("archivezz.zip")), is(notNullValue()));
    }


}