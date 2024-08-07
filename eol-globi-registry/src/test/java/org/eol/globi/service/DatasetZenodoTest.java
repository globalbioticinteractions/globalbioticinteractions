package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetZenodoTest {

    @Rule

    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void doi() throws IOException {
        File file = folder.newFolder();
        DatasetZenodo datasetZenodo = new DatasetZenodo("some/namespace", URI.create("https://zenodo.org/record/1234/blabla"), new InputStreamFactoryNoop(), file);
        assertThat(datasetZenodo.getDOI().toString(), is("10.5281/zenodo.1234"));
    }

}