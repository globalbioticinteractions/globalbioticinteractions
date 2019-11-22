package org.eol.globi.service;

import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;

public class DatasetRegistryZenodoTest {

    @Test
    public void extractGitHubRepos() throws DatasetFinderException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-1-item.xml");
        Collection<String> relations = DatasetRegistryZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetRegistryZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(1));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void extractGitHubRepos3() throws DatasetFinderException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-3-items.xml");
        Collection<String> relations = DatasetRegistryZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetRegistryZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(3));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void findMatchingGithub() throws IOException, XPathExpressionException, DatasetFinderException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-3-items.xml");
        NodeList records = DatasetRegistryZenodo.getRecordNodeList(resourceAsStream);
        URI uri = DatasetRegistryZenodo.findZenodoGitHubArchives(records, "jhammock/Layman-and-Allgeier-Lionfish");
        assertThat(uri.toString(), is("https://zenodo.org/record/232498/files/jhammock/Layman-and-Allgeier-Lionfish-1.0.zip"));
        uri = DatasetRegistryZenodo.findZenodoGitHubArchives(records, "globalbioticinteractions/template-dataset");
        assertThat(uri.toString(), is("https://zenodo.org/record/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
    }

}