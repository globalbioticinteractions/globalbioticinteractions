package org.globalbioticinteractions.dataset;

import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentPathFactoryDepth0;
import org.globalbioticinteractions.cache.ProvenancePathFactoryImpl;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertNotNull;

public class DatasetRegistryWithCacheIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void zenodoTest() throws DatasetRegistryException, IOException {
        assertTemplateDataset("zenodo.org",
                new DatasetRegistryZenodo(getResourceService()),
                "Jorrit H. Poelen. 2014. Species associations manually extracted from literature.");
    }

    private ResourceService getResourceService() throws IOException {
        return new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
    }

    @Test
    public void templateDatasetGithub() throws DatasetRegistryException, IOException {
        assertTemplateDataset("github.com",
                new DatasetRegistryGitHubArchive(getResourceService()),
                "Jorrit H. Poelen. 2014. Species associations manually extracted from literature.");
    }

    private void assertTemplateDataset(String expectedURIFragment, DatasetRegistry datasetRegistry, String expectedCitation) throws DatasetRegistryException, IOException {
        final File cacheDir = folder.newFolder();
        DatasetRegistry finder = new DatasetRegistryWithCache(datasetRegistry, dataset -> {
            return CacheUtil.cacheFor(dataset.getNamespace(),
                    cacheDir.getAbsolutePath(),
                    cacheDir.getAbsolutePath(),
                    new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), cacheDir),
                    new ResourceServiceLocal(new InputStreamFactoryNoop()),
                    new ContentPathFactoryDepth0(),
                    new ProvenancePathFactoryImpl()
            );
        });

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/template-dataset");

        assertThat(dataset.getArchiveURI().toString(), containsString(expectedURIFragment));
        assertThat(dataset.retrieve(URI.create("globi.json")), Is.is(notNullValue()));
        assertThat(dataset.getCitation(), startsWith(expectedCitation));
    }


    @Test
    public void afrotropicalBees() throws DatasetRegistryException, IOException {
        final File cacheDir = folder.newFolder();
        DatasetRegistry finder = new DatasetRegistryWithCache(
                new DatasetRegistryGitHubArchive(getResourceService()),
                dataset -> CacheUtil.cacheFor(
                        dataset.getNamespace(),
                        cacheDir.getAbsolutePath(),
                        cacheDir.getAbsolutePath(),
                        new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), cacheDir),
                        new ResourceServiceLocal(new InputStreamFactoryNoop()),
                        new ContentPathFactoryDepth0(),
                        new ProvenancePathFactoryImpl()
                )
        );

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/Catalogue-of-Afrotropical-Bees");

        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
        assertThat(dataset.retrieve(URI.create("globi.json")), Is.is(notNullValue()));
        assertThat(dataset.getCitation(), startsWith("Eardley C, Coetzer W. 2016. Catalogue of Afrotropical Bees."));

    }

    @Test
    public void hafnerTest() throws DatasetRegistryException, IOException {
        final File cacheDir = folder.newFolder();

        DatasetRegistry finder = new DatasetRegistryWithCache(new DatasetRegistryGitHubArchive(getResourceService()),
                dataset -> CacheUtil.cacheFor(
                        dataset.getNamespace(),
                        cacheDir.getAbsolutePath(),
                        cacheDir.getAbsolutePath(),
                        new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), cacheDir),
                        new ResourceServiceLocal(new InputStreamFactoryNoop()),
                        new ContentPathFactoryDepth0(),
                        new ProvenancePathFactoryImpl()
                )
        );

        Dataset dataset = new DatasetFactory(finder).datasetFor("globalbioticinteractions/hafner");

        try (InputStream resource = dataset.retrieve(URI.create("hafner/gopher_lice_int.csv"))) {
            assertNotNull(resource);
        }


    }
}
