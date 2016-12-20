package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class DatasetFinderZenodoTest {

    @Test
    public void zenodoDataFeed() throws DatasetFinderException, IOException {
        String feed = IOUtils.toString(DatasetFinderZenodo.getFeed(), "UTF-8");
        assertThat(feed, containsString("<?xml version"));
    }

    @Test
    public void extractGitHubRepos() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, DatasetFinderException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-results.xml");
        Collection<String> relations = DatasetFinderZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetFinderZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(1));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void extractGitHubReposArchives() throws DatasetFinderException {
        URI uri = new DatasetFinderZenodo().datasetFor("globalbioticinteractions/template-dataset").getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
    }


}