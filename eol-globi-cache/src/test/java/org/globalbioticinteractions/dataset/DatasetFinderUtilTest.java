package org.globalbioticinteractions.dataset;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetFinderUtilTest {

    @Test
    public void findArchiveRoot() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("archive.zip");
        URI localDatasetURIRoot = DatasetFinderUtil.getLocalDatasetURIRoot(new File(resource.toURI()));
        assertThat(localDatasetURIRoot.toString(), endsWith("/archive.zip!/template-dataset-e68f4487ebc3bc70668c0f738223b92da0598c00/"));
    }

    @Test
    public void findArchiveRootAgain() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("coldp-hepialidae.zip");
        URI localDatasetURIRoot = DatasetFinderUtil.getLocalDatasetURIRoot(new File(resource.toURI()));
        assertThat(localDatasetURIRoot.toString(), endsWith("/coldp-hepialidae.zip!/Hepialidae_1.0/"));
    }
    @Test
    public void findArchiveRootOneFileAgain() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("onefile.zip");
        URI localDatasetURIRoot = DatasetFinderUtil.getLocalDatasetURIRoot(new File(resource.toURI()));
        assertThat(localDatasetURIRoot.toString(), endsWith("/onefile.zip!/"));
    }

    @Test
    public void findArchiveRootWithFileInRoot() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("coldp-2207.zip");
        URI localDatasetURIRoot = DatasetFinderUtil.getLocalDatasetURIRoot(new File(resource.toURI()));
        assertThat(localDatasetURIRoot.toString(), endsWith("coldp-2207.zip!/"));
    }

}