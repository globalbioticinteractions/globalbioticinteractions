package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetTest {



    @Test
    public void resourceURL() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("archive.zip");
        File archive = new File(resource.toURI());
        assertThat(archive.exists(), is(true));

        URI parentDir = archive.getParentFile().toURI();

        Dataset dataset = new DatasetWithResourceMapping("some/namespace", parentDir, new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        InputStream retrieve = dataset.retrieve(resource.toURI());
        assertArchiveHash(retrieve);
        assertArchiveHash(dataset.retrieve(URI.create("archive.zip")));
        assertArchiveHash(dataset.retrieve(URI.create("/archive.zip")));
    }

    private void assertArchiveHash(InputStream retrieve) throws IOException {
        TestHashUtil.assertContentHash(retrieve, "c9ecb3b0100c890bd00a5c201d06f0a78d92488591f726fbf4de5c88bda39147");
    }

    @Test(expected = IOException.class)
    public void unknownResourceURL() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("archive.zip");
        File archive = new File(resource.toURI());
        assertThat(archive.exists(), is(true));

        URI parentDir = archive.getParentFile().toURI();

        Dataset dataset = new DatasetWithResourceMapping("some/namespace", parentDir, new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        assertThat(dataset.retrieve(URI.create("archivezz.zip")), is(notNullValue()));
    }


}