package org.globalbioticinteractions.dataset;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFinderException;
import org.globalbioticinteractions.cache.CacheUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class DatasetFinderLocalTest {

    private DatasetFinderLocal datasetFinderLocal;

    @Before
    public void init() throws URISyntaxException {
        URL accessFile = getClass().getResource("/test-cache/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(accessFile);
        File cacheDir = new File(accessFile.toURI()).getParentFile().getParentFile().getParentFile();
        datasetFinderLocal = new DatasetFinderLocal(cacheDir.getAbsolutePath(), dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cacheDir.getAbsolutePath()));
    }

    @Test
    public void findNamespaces() throws DatasetFinderException {
        assertThat(datasetFinderLocal.findNamespaces(), hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void dataset() throws DatasetFinderException, URISyntaxException {
        Dataset actual = datasetFinderLocal.datasetFor("globalbioticinteractions/template-dataset");
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getConfigURI().toString(), endsWith("/test-cache/globalbioticinteractions/template-dataset/6bfc17b8717e6e8e478552f12404bc8887d691a155ffd9cd9bfc80cb6747c5d2!/template-dataset-8abd2ba18457288f33527193299504015fae6def/globi.json"));
        assertThat(actual.getArchiveURI().toString(), is("https://github.com/globalbioticinteractions/template-dataset/archive/8abd2ba18457288f33527193299504015fae6def.zip"));
        assertThat(actual.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature. Accessed on 2017-09-14T16:45:38Z via <https://github.com/globalbioticinteractions/template-dataset/archive/8abd2ba18457288f33527193299504015fae6def.zip>."));
    }

    @Test(expected = DatasetFinderException.class)
    public void nonExistingDataset() throws DatasetFinderException {
        datasetFinderLocal.datasetFor("non/existing");
    }

}