package org.globalbioticinteractions.dataset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.xpath.XPathExpressionException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DatasetRegistryZenodoIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Ignore("datacite3 no longer supported")
    @Test
    public void zenodoDataFeedDatacite3() throws DatasetRegistryException {
        String metadataPrefix = "oai_datacite3";
        String feed = DatasetRegistryZenodo.getNextPage(
                null,
                getResourceService(),
                metadataPrefix);
        assertThat(feed, containsString("<?xml version"));
        assertThat(feed, containsString("metadataPrefix=\"" + metadataPrefix + "\""));
    }

    private ResourceService getResourceService() throws DatasetRegistryException {
        try {
            return new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
        } catch (IOException e) {
            throw new DatasetRegistryException(e);
        }
    }

    @Test
    public void zenodoDataFeedDatacite4() throws DatasetRegistryException {
        String feed = DatasetRegistryZenodo.getNextPage(
                null,
                getResourceService(),
                "oai_datacite");
        assertThat(feed, containsString("<?xml version"));
        assertThat(feed, containsString("metadataPrefix=\"" + "oai_datacite" + "\""));
    }

    @Test
    public void zenodoDataFeedListDatacite4() throws DatasetRegistryException, XPathExpressionException, MalformedURLException {
        List<String> feed = DatasetRegistryZenodo.getCachedPages(
                null,
                getResourceService(),
                "oai_datacite");

        Map<String, List<Pair<Long, URI>>> zenodoArchives = DatasetRegistryZenodo.findZenodoArchives(feed.stream().map(str -> IOUtils.toInputStream(str, StandardCharsets.UTF_8)).collect(Collectors.toList()));

        assertThat(zenodoArchives, hasKey("mdrishti/pushPullIntxn"));

        List<Pair<Long, URI>> pairs = zenodoArchives.get("mdrishti/pushPullIntxn");

        for (Pair<Long, URI> pair : pairs) {
            System.out.println(pair);
        }
    }

    @Test
    public void unlikelyMatch() throws DatasetRegistryException {
        DatasetRegistryZenodo datasetRegistryZenodo = new DatasetRegistryZenodo(getResourceService());
        Dataset thisshouldnotexist = datasetRegistryZenodo.datasetFor("thisshouldnotexist");
        assertNull(thisshouldnotexist);
    }

    @Test
    public void extractGitHubReposArchives() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(getResourceService())
                .datasetFor("globalbioticinteractions/template-dataset");

        assertNotNull(dataset);
        URI uri = dataset
                .getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(),
                is("https://zenodo.org/records/1436853/files/globalbioticinteractions/template-dataset-0.0.3.zip"));
    }

    @Test
    public void extractGitHubReposArchives2() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(getResourceService())
                .datasetFor("millerse/Lara-C.-2006");

        assertThat(dataset, is(notNullValue()));
        URI uri = dataset
                .getArchiveURI();
        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/records/258208/files/millerse/Lara-C.-2006-v1.0.zip"));
    }

    @Test
    public void extractGitHubReposArchives3() throws DatasetRegistryException {
        Dataset dataset = new DatasetRegistryZenodo(getResourceService())
                .datasetFor("millerse/Lichenous");

        assertThat(dataset, is(notNullValue()));

        URI uri = dataset
                .getArchiveURI();

        assertThat(uri, is(notNullValue()));
        assertThat(uri.toString(), is("https://zenodo.org/records/545807/files/millerse/Lichenous-v2.0.0.zip"));
    }

}