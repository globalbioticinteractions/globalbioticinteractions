package org.globalbioticinteractions.dataset;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;

public class DatasetFinderUtilTest {

    @Test
    public void findArchiveRoot() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("archive.zip");
        URI localDatasetURIRoot = DatasetFinderUtil.getLocalDatasetURIRoot(new File(resource.toURI()));
        assertThat(localDatasetURIRoot.toString(), endsWith("/archive.zip!/template-dataset-e68f4487ebc3bc70668c0f738223b92da0598c00/"));
    }

}