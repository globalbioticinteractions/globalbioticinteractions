package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetZenodoTest {

    @Test
    public void doi() {
        DatasetZenodo datasetZenodo = new DatasetZenodo("some/namespace", URI.create("https://zenodo.org/record/1234/blabla"), new InputStreamFactoryNoop());
        assertThat(datasetZenodo.getDOI().toString(), is("10.5281/zenodo.1234"));
    }

}