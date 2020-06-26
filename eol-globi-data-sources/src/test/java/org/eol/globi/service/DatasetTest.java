package org.eol.globi.service;

import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.TestCase.fail;
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
        TestHashUtil.assertContentHash(dataset.retrieve(resource.toURI()), "c9ecb3b0100c890bd00a5c201d06f0a78d92488591f726fbf4de5c88bda39147");
        TestHashUtil.assertContentHash(dataset.retrieve(URI.create("archive.zip")), "c9ecb3b0100c890bd00a5c201d06f0a78d92488591f726fbf4de5c88bda39147");
        TestHashUtil.assertContentHash(dataset.retrieve(URI.create("/archive.zip")), "c9ecb3b0100c890bd00a5c201d06f0a78d92488591f726fbf4de5c88bda39147");
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