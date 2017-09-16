package org.globalbioticinteractions.dataset;

import org.apache.commons.io.FileUtils;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFactory;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetFinderException;
import org.eol.globi.service.DatasetFinderGitHubArchive;
import org.eol.globi.service.DatasetFinderZenodo;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class DatasetFinderCachingIT {

    private String cachePath = "target/cache/datasets";

    @Before
    public void deleteCacheDir() {
        FileUtils.deleteQuietly(new File(cachePath));
    }

    @Test
    public void zenodoTest() throws DatasetFinderException, IOException {
        assertTemplateDataset("zenodo.org",
                new DatasetFinderZenodo(),
                "Jorrit H. Poelen. 2014. Species associations manually extracted from literature. <https://doi.org/10.5281/zenodo.207958>. Accessed on");
    }

    @Test
    public void githubTest() throws DatasetFinderException, IOException {
        assertTemplateDataset("github.com",
                new DatasetFinderGitHubArchive(),
                "Jorrit H. Poelen. 2014. Species associations manually extracted from literature. Accessed on");
    }

    private void assertTemplateDataset(String expectedURIFragment, DatasetFinder datasetFinder, String expectedCitation) throws DatasetFinderException {
        DatasetFinder finder = new DatasetFinderCaching(datasetFinder, cachePath);

        Dataset dataset = DatasetFactory.datasetFor("globalbioticinteractions/template-dataset", finder);

        assertThat(dataset.getArchiveURI().toString(), containsString(expectedURIFragment));
        assertThat(dataset.getResourceURI("globi.json").toString(), startsWith("jar:file:/"));
        assertThat(dataset.getCitation(), startsWith(expectedCitation));
    }


    @Test
    public void gitHubTest() throws DatasetFinderException {
        DatasetFinder finder = new DatasetFinderCaching(new DatasetFinderGitHubArchive(), cachePath);

        Dataset dataset = DatasetFactory.datasetFor("globalbioticinteractions/Catalogue-of-Afrotropical-Bees", finder);

        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
        assertThat(dataset.getResourceURI("globi.json").toString(), startsWith("jar:file:/"));
        assertThat(dataset.getCitation(), startsWith("Eardley C, Coetzer W. 2011. Catalogue of Afrotropical Bees. <http://doi.org/10.15468/u9ezbh>. Accessed on"));

    }
}
