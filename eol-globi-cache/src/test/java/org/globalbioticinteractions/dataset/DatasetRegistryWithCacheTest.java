package org.globalbioticinteractions.dataset;

import org.apache.commons.io.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CacheUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class DatasetRegistryWithCacheTest {

    private static final String cachePath = "target/cache-test";

    @Before
    @After
    public void mkdirs() {
        FileUtils.deleteQuietly(new File(cachePath));
    }

    @Test
    public void cacheDatasetLocal() throws IOException, URISyntaxException {
        Dataset datasetCached = datasetCached();

        assertNotNull(datasetCached.getArchiveURI());

        try (InputStream is = datasetCached.retrieve(URI.create("globi.json"))) {
            assertNotNull(is);
            JsonNode jsonNode = new ObjectMapper().readTree(is);
            assertThat(jsonNode.has("citation"), is(true));
        }


        String[] list = new File(cachePath + "/some/namespace").list();
        assertThat(list.length, is(2));
        assertThat(Arrays.asList(list), hasItem("c9ecb3b0100c890bd00a5c201d06f0a78d92488591f726fbf4de5c88bda39147"));
        assertThat(Arrays.asList(list), hasItem("access.tsv"));
    }


    private Dataset datasetCached() throws IOException, URISyntaxException {
        Dataset dataset = Mockito.mock(Dataset.class);
        when(dataset.getNamespace()).thenReturn("some/namespace");
        when(dataset.getArchiveURI()).thenReturn(getClass().getResource("archive.zip").toURI());
        Cache cache = CacheUtil.cacheFor("some/namespace", cachePath, new ResourceServiceLocal(new InputStreamFactoryNoop()), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        return new DatasetWithCache(dataset, cache);
    }
}
