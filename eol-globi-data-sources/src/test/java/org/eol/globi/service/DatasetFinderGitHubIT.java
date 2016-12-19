package org.eol.globi.service;

import org.junit.Test;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class DatasetFinderGitHubIT {

    @Test
    public void discoverDatasetsInGitHub() throws DatasetFinderException {
        Collection<String> urls = new DatasetFinderGitHub().find();
        assertThat(urls.size(), is(not(0)));
    }

    @Test
    public void archiveUrlFor() throws DatasetFinderException {
        URL url = new DatasetFinderGitHub().archiveUrlFor("globalbioticinteractions/template-dataset");
        assertThat(url.toString(), startsWith("https://github.com/globalbioticinteractions/template-dataset/archive/"));
        assertThat(url.toString(), endsWith(".zip"));
    }

}