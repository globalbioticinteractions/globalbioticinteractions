package org.globalbioticinteractions.elton;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class Elton4NTestUtil {


    public static String getTestDatasetDir() throws URISyntaxException {
        URL resource = Elton4NTestUtil.class.getResource("/datasets/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(resource);
        return new File(resource.toURI()).getParentFile().getParentFile().getParentFile().getAbsolutePath();
    }


}