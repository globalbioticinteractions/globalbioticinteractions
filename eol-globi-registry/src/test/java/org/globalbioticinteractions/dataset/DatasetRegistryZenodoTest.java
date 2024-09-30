package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.globalbioticinteractions.dataset.DatasetRegistryZenodo.findZenodoGitHubArchives;
import static org.globalbioticinteractions.dataset.DatasetRegistryZenodo.getRecordNodeList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class DatasetRegistryZenodoTest {

    @Test
    public void extractGitHubRepos() throws DatasetRegistryException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-1-item.xml");
        Collection<String> relations = DatasetRegistryZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetRegistryZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(1));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void extractGitHubReposPage1() throws DatasetRegistryException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-page1.xml");
        Collection<String> relations = DatasetRegistryZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetRegistryZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(17));
        assertThat(refs, not(hasItem("globalbioticinteractions/template-dataset")));
    }

    @Test
    public void extractGitHubReposPage2() throws DatasetRegistryException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-page2.xml");
        Collection<String> relations = DatasetRegistryZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetRegistryZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(42));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void extractGitHubRepos3() throws DatasetRegistryException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-3-items.xml");
        Collection<String> relations = DatasetRegistryZenodo.getRelations(resourceAsStream);
        Collection<String> refs = DatasetRegistryZenodo.findPublishedGitHubRepos(relations);
        assertThat(refs.size(), is(3));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void findMatchingGithub() throws IOException, XPathExpressionException, DatasetRegistryException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-3-items.xml");
        NodeList records = getRecordNodeList(resourceAsStream);
        URI uri = DatasetRegistryZenodo.findLatestZenodoGitHubArchiveForNamespace(records, "jhammock/Layman-and-Allgeier-Lionfish");
        assertThat(uri.toString(), is("https://zenodo.org/records/232498/files/jhammock/Layman-and-Allgeier-Lionfish-1.0.zip"));
        uri = DatasetRegistryZenodo.findLatestZenodoGitHubArchiveForNamespace(records, "globalbioticinteractions/template-dataset");
        assertThat(uri.toString(), is("https://zenodo.org/records/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
    }

    @Test
    public void noMatchingGithub() throws IOException, XPathExpressionException, DatasetRegistryException {
        InputStream resourceAsStream = getClass().getResourceAsStream("zenodo-oai-request-result-3-items.xml");
        NodeList records = getRecordNodeList(resourceAsStream);
        URI uri = DatasetRegistryZenodo.findLatestZenodoGitHubArchiveForNamespace(records, "jhammock/Layman-and-Allgeier-Lionfish");
        assertThat(uri.toString(), is("https://zenodo.org/records/232498/files/jhammock/Layman-and-Allgeier-Lionfish-1.0.zip"));
        uri = DatasetRegistryZenodo.findLatestZenodoGitHubArchiveForNamespace(records, "globalbioticinteractions/template-dataset");
        assertThat(uri.toString(), is("https://zenodo.org/records/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
    }

    @Test
    public void parseResumptionToken() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        InputStream is = getClass().getResourceAsStream("zenodo-oai-request-page1.xml");
        String resumptionToken = DatasetRegistryZenodo.parseResumptionToken(is);
        assertThat(resumptionToken, is(".eJwVjdEKgjAYhd_lv66YpaKCF0kpBFFaanUjU9caThfbolB699a5PIfvOxMoQloI0MJ1bN_2fORanu3a_nIGqpGC84qZGTaX2Lpddk5aOl3WF-i6pNH6nyg-z6NtWSePMYt5fi0f2aloDzW6OZkVJfu-G_M0DGEGT0wJBMbbvbGkCoLJfGvjfiki55SLGvOaCc0aNmgicaOZGJQBe6JxizU-SnJnHwMIzKp_0TBNVvD9_gDwrz-E.YT-SJQ.FVlu6cSWwJjBp3EhxXgxz0kmVCA"));
    }

    @Test
    public void parseResumptionTokenNonExisting() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        InputStream is = getClass().getResourceAsStream("zenodo-oai-request-page-final.xml");
        String resumptionToken = DatasetRegistryZenodo.parseResumptionToken(is);
        assertThat(resumptionToken, is(nullValue()));
    }

    @Test
    public void generateResumptionURI() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        String resumptionToken = ".eJwVjdEKgjAYhd_lv66YpaKCF0kpBFFaanUjU9caThfbolB699a5PIfvOxMoQloI0MJ1bN_2fORanu3a_nIGqpGC84qZGTaX2Lpddk5aOl3WF-i6pNH6nyg-z6NtWSePMYt5fi0f2aloDzW6OZkVJfu-G_M0DGEGT0wJBMbbvbGkCoLJfGvjfiki55SLGvOaCc0aNmgicaOZGJQBe6JxizU-SnJnHwMIzKp_0TBNVvD9_gDwrz-E.YT-SJQ.FVlu6cSWwJjBp3EhxXgxz0kmVCA";

        URI uri = DatasetRegistryZenodo.generateResumptionURI(resumptionToken);

        assertThat(uri.toString(), is("https://zenodo.org/oai2d?verb=ListRecords&resumptionToken=.eJwVjdEKgjAYhd_lv66YpaKCF0kpBFFaanUjU9caThfbolB699a5PIfvOxMoQloI0MJ1bN_2fORanu3a_nIGqpGC84qZGTaX2Lpddk5aOl3WF-i6pNH6nyg-z6NtWSePMYt5fi0f2aloDzW6OZkVJfu-G_M0DGEGT0wJBMbbvbGkCoLJfGvjfiki55SLGvOaCc0aNmgicaOZGJQBe6JxizU-SnJnHwMIzKp_0TBNVvD9_gDwrz-E.YT-SJQ.FVlu6cSWwJjBp3EhxXgxz0kmVCA"));
    }

    @Test
    public void newerVersion() throws DatasetRegistryException, XPathExpressionException, MalformedURLException {
        Map<String, List<Pair<Long, URI>>> zenodoArchives = getZenodoArchives();

        List<Pair<Long, URI>> pairs = getZenodoArchives().get("mdrishti/pushPullIntxn");

        List<Long> ids = pairs
                .stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());

        assertThat(ids, hasItem(13286377L));
        assertThat(ids, hasItem(13123981L));
        assertThat(ids, hasItem(13121135L));

        URI mostRecentArchiveInNamespace = DatasetRegistryZenodo.getMostRecentArchiveInNamespace("mdrishti/pushPullIntxn", zenodoArchives);

        assertThat(mostRecentArchiveInNamespace, is(URI.create("https://zenodo.org/records/13286377/files/mdrishti/pushPullIntxn-v0.1.1.zip")));


    }

    @Test
    public void newerVersionURI() throws DatasetRegistryException, XPathExpressionException, MalformedURLException {
        Map<String, List<Pair<Long, URI>>> zenodoArchives = getZenodoArchives();

        URI mostRecentArchiveInNamespace = DatasetRegistryZenodo.getMostRecentArchiveInNamespace(
                "mdrishti/pushPullIntxn",
                zenodoArchives
        );

        assertThat(mostRecentArchiveInNamespace,
                is(URI.create("https://zenodo.org/records/13286377/files/mdrishti/pushPullIntxn-v0.1.1.zip"))
        );


    }

    private Map<String, List<Pair<Long, URI>>> getZenodoArchives() throws DatasetRegistryException, XPathExpressionException, MalformedURLException {
        List<InputStream> inputStreams = new ArrayList<>();
        for (int pageNumber = 1; pageNumber < 7; pageNumber++) {
            inputStreams.add(getClass().getResourceAsStream("request/page" + pageNumber + ".xml"));
        }

        return DatasetRegistryZenodo.findZenodoArchives(inputStreams);
    }

    @Test
    public void findInPage() throws DatasetRegistryException, XPathExpressionException {
        Map<String, List<Pair<Long, URI>>> zenodoArchives = findZenodoGitHubArchives(getRecordNodeList(getClass().getResourceAsStream("request/page4.xml")));

        assertThat(zenodoArchives, hasKey("mdrishti/pushPullIntxn"));

        List<Pair<Long, URI>> pairs = zenodoArchives.get("mdrishti/pushPullIntxn");

        List<Long> ids = pairs
                .stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());

        assertThat(ids, hasItem(13286377L));
    }

}