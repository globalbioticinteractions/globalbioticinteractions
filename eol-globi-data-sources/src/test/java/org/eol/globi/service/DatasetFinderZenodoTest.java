package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.NodeList;
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
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-1-item.xml");
        Collection<String> relations = DatasetFinderZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetFinderZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(1));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void extractGitHubRepos3() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, DatasetFinderException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-3-items.xml");
        Collection<String> relations = DatasetFinderZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetFinderZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(3));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void findMatchingGithub() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, DatasetFinderException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-3-items.xml");
        NodeList records = DatasetFinderZenodo.getRecordNodeList(resourceAsStream);
        URI uri = DatasetFinderZenodo.findZenodoGitHubArchives(records, "jhammock/Layman-and-Allgeier-Lionfish");
        assertThat(uri.toString(), is("https://zenodo.org/record/232498/files/jhammock/Layman-and-Allgeier-Lionfish-1.0.zip"));
        uri = DatasetFinderZenodo.findZenodoGitHubArchives(records, "globalbioticinteractions/template-dataset");
        assertThat(uri.toString(), is("https://zenodo.org/record/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
    }

    @Test
    public void extractGitHubReposArchives() throws DatasetFinderException {
        URI uri = new DatasetFinderZenodo().datasetFor("globalbioticinteractions/template-dataset").getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
    }

    @Test
    public void extractGitHubReposArchives2() throws DatasetFinderException {
        URI uri = new DatasetFinderZenodo().datasetFor("millerse/Lara-C.-2006").getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/258208/files/millerse/Lara-C.-2006-v1.0.zip"));
    }

    @Test
    public void extractGitHubReposArchives3() throws DatasetFinderException {
        URI uri = new DatasetFinderZenodo().datasetFor("millerse/Lichenous").getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/record/545807/files/millerse/Lichenous-v2.0.0.zip"));
    }

}