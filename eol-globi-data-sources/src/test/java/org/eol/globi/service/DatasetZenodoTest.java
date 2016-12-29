package org.eol.globi.service;

import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DatasetZenodoTest {

    @Test
    public void doi() {
        DatasetZenodo datasetZenodo = new DatasetZenodo("some/namespace", URI.create("https://zenodo.org/record/1234/blabla"));
        assertThat(datasetZenodo.getDOI(), is("https://doi.org/10.5281/zenodo.1234"));
    }

}