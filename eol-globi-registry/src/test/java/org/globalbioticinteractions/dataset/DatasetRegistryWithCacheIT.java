package org.globalbioticinteractions.dataset;

import org.apache.commons.io.FileUtils;
import org.globalbioticinteractions.cache.CacheUtil;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetRegistryWithCacheIT {

    private String cachePath = "target/cache/datasets";

    @Before
    public void deleteCacheDir() {
        FileUtils.deleteQuietly(new File(cachePath));
    }

    @Test
    public void zenodoTest() throws DatasetRegistryException, IOException {
        assertTemplateDataset("zenodo.org",
                new DatasetRegistryZenodo(inStream -> inStream),
                "Jorrit H. Poelen. 2014. Species associations manually extracted from literature.");
    }

    @Test
    public void templateDatasetGithub() throws DatasetRegistryException, IOException {
        assertTemplateDataset("github.com",
                new DatasetRegistryGitHubArchive(inStream -> inStream),
                "Jorrit H. Poelen. 2014. Species associations manually extracted from literature.");
    }

    private void assertTemplateDataset(String expectedURIFragment, DatasetRegistry datasetRegistry, String expectedCitation) throws DatasetRegistryException, IOException {
        DatasetRegistry finder = new DatasetRegistryWithCache(datasetRegistry, dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cachePath, inStream -> inStream));

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/template-dataset");

        assertThat(dataset.getArchiveURI().toString(), containsString(expectedURIFragment));
        assertThat(dataset.retrieve(URI.create("globi.json")), Is.is(notNullValue()));
        assertThat(dataset.getCitation(), startsWith(expectedCitation));
    }


    @Test
    public void afrotropicalBees() throws DatasetRegistryException, IOException {
        DatasetRegistry finder = new DatasetRegistryWithCache(new DatasetRegistryGitHubArchive(inStream -> inStream), dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cachePath, inStream -> inStream));

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/Catalogue-of-Afrotropical-Bees");

        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
        assertThat(dataset.retrieve(URI.create("globi.json")), Is.is(notNullValue()));
        assertThat(dataset.getCitation(), startsWith("Eardley C, Coetzer W. 2011. Catalogue of Afrotropical Bees."));

    }

    @Test
    public void hafnerTest() throws DatasetRegistryException, IOException {
        DatasetRegistry finder = new DatasetRegistryWithCache(new DatasetRegistryGitHubArchive(inStream -> inStream),
                dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cachePath, inStream -> inStream));

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/hafner");

        try (InputStream resource = dataset.retrieve(URI.create("hafner/gopher_lice_int.csv"))) {
            assertNotNull(resource);
        }


    }
}
