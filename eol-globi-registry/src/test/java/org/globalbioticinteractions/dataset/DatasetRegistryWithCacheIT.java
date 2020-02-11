package org.globalbioticinteractions.dataset;

import org.apache.commons.io.FileUtils;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetRegistryGitHubArchive;
import org.eol.globi.service.DatasetRegistryZenodo;
import org.globalbioticinteractions.cache.CacheUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class DatasetRegistryWithCacheIT {

    private String cachePath = "target/cache/datasets";

    @Before
    public void deleteCacheDir() {
        FileUtils.deleteQuietly(new File(cachePath));
    }

    @Test
    public void zenodoTest() throws DatasetFinderException, IOException {
        assertTemplateDataset("zenodo.org",
                new DatasetRegistryZenodo(inStream -> inStream),
                "Jorrit H. Poelen. 2014. Species associations manually extracted from literature. <https://doi.org/10.5281/zenodo.207958>. Accessed on");
    }

    @Test
    public void githubTest() throws DatasetFinderException, IOException {
        assertTemplateDataset("github.com",
                new DatasetRegistryGitHubArchive(inStream -> inStream),
                "Jorrit H. Poelen. 2014. Species associations manually extracted from literature. Accessed on");
    }

    private void assertTemplateDataset(String expectedURIFragment, DatasetRegistry datasetRegistry, String expectedCitation) throws DatasetFinderException, IOException {
        DatasetRegistry finder = new DatasetRegistryWithCache(datasetRegistry, dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cachePath, inStream -> inStream));

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/template-dataset");

        assertThat(dataset.getArchiveURI().toString(), containsString(expectedURIFragment));
        assertThat(dataset.getLocalURI(URI.create("globi.json")).toString(), startsWith("jar:file:/"));
        assertThat(dataset.getCitation(), startsWith(expectedCitation));
    }


    @Test
    public void gitHubTest() throws DatasetFinderException, IOException {
        DatasetRegistry finder = new DatasetRegistryWithCache(new DatasetRegistryGitHubArchive(inStream -> inStream), dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cachePath, inStream -> inStream));

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/Catalogue-of-Afrotropical-Bees");

        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
        assertThat(dataset.getLocalURI(URI.create("globi.json")).toString(), startsWith("jar:file:/"));
        assertThat(dataset.getCitation(), startsWith("Shan Kothari, Pers. Comm. 2014."));

    }

    @Test
    public void hafnerTest() throws DatasetFinderException, IOException {
        DatasetRegistry finder = new DatasetRegistryWithCache(new DatasetRegistryGitHubArchive(inStream -> inStream),
                dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cachePath, inStream -> inStream));

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/hafner");

        try (InputStream resource = dataset.retrieve(URI.create("hafner/gopher_lice_int.csv"))) {
            assertNotNull(resource);
        }


    }
}
