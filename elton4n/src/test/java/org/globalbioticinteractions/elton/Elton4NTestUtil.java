package org.globalbioticinteractions.elton;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class Elton4NTestUtil {


    private static String getTestDatasetDir() throws URISyntaxException {
        URL resource = Elton4NTestUtil.class.getResource("/datasets/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(resource);
        return new File(resource.toURI()).getParentFile().getParentFile().getParentFile().getAbsolutePath();
    }



    public static void assertCompileLinkExport(String neo4jVersion, File folder) throws URISyntaxException, IOException {
        assertThat(
                Elton4N.run(new String[]{
                        "compile",
                        "-datasetDir", getTestDatasetDir(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "-neo4jVersion", neo4jVersion,
                        "link",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "-neo4jVersion", neo4jVersion,
                }),
                Is.is(0)
        );

        assertThat(
                Elton4N.run(new String[]{
                        "package",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "-neo4jVersion", neo4jVersion
                }),
                Is.is(0)
        );
    }

}