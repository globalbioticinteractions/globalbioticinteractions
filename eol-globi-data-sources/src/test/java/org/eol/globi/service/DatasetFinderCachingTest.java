package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class DatasetFinderCachingTest {

    @Test
    public void cacheDatasetGitHub() throws DatasetFinderException, IOException {
        Dataset dataset = new DatasetFinderGitHubArchive()
                .datasetFor("globalbioticinteractions/template-dataset");
        File archiveCache = DatasetFinderCaching.cache(dataset, "target/datasets/cache");
        assertThat(archiveCache.exists(), is(true));
        assertThat(archiveCache.toURI().toString(), startsWith("file:/"));
    }

    @Test
    public void cacheDatasetLocal() throws DatasetFinderException, IOException, URISyntaxException {
        Dataset dataset = new DatasetRemote("some/namespace", URI.create("http://example.com"));
        Dataset datasetCached = DatasetFinderCaching.cacheArchive(dataset, new File(getClass().getResource("archive.zip").toURI()));

        URI uri = datasetCached.getResourceURI("globi.json");
        assertThat(uri.isAbsolute(), is(true));
        assertThat(uri.toString(), startsWith("jar:file:"));

        InputStream is = datasetCached.getResource("globi.json");
        JsonNode jsonNode = new ObjectMapper().readTree(is);
        assertThat(jsonNode.has("citation"), is(true));
    }


}