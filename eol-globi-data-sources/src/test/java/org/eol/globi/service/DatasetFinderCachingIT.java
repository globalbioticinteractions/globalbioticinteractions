package org.eol.globi.service;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class DatasetFinderCachingIT {

    @Test
    public void zenodoTest() throws DatasetFinderException {
        DatasetFinder finder = new DatasetFinderCaching(new DatasetFinderZenodo());

        Dataset dataset = DatasetFactory.datasetFor("globalbioticinteractions/template-dataset", finder);

        assertThat(dataset.getArchiveURI().toString(), containsString("zenodo.org"));
        assertThat(dataset.getResourceURI("globi.json").toString(), startsWith("jar:file:/"));
        assertThat(dataset.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature. https://doi.org/10.5281/zenodo.207958"));

    }

    @Test
    public void cacheDatasetGitHub() throws DatasetFinderException, IOException {
        Dataset dataset = new DatasetFinderGitHubArchive()
                .datasetFor("globalbioticinteractions/template-dataset");
        File archiveCache = DatasetFinderCaching.cache(dataset, "target/cache/dataset");
        assertThat(archiveCache.exists(), CoreMatchers.is(true));
        assertThat(archiveCache.toURI().toString(), startsWith("file:/"));
    }

    @Test
    public void gitHubTest() throws DatasetFinderException {
        DatasetFinder finder = new DatasetFinderCaching(new DatasetFinderGitHubArchive());

        Dataset dataset = DatasetFactory.datasetFor("globalbioticinteractions/Catalogue-of-Afrotropical-Bees", finder);

        assertThat(dataset.getArchiveURI().toString(), containsString("github.com"));
        assertThat(dataset.getResourceURI("globi.json").toString(), startsWith("jar:file:/"));
        assertThat(dataset.getCitation(), is("Eardley C, Coetzer W. 2011. Catalogue of Afrotropical Bees. http://doi.org/10.15468/u9ezbh"));

    }


}