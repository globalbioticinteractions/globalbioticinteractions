package org.globalbioticinteractions.elton;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class Elton4NTestUtil {


    private static String getTestDatasetDir() throws URISyntaxException {
        URL resource = Elton4NTestUtil.class.getResource("/datasets/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(resource);
        return new File(resource.toURI()).getParentFile().getParentFile().getParentFile().getAbsolutePath();
    }



    public static void assertCompileLinkExport(String neo4jVersion, File folder, String expectedOutput) throws URISyntaxException, IOException {
        String nameIndexCache = new File(folder, "nameIndexCache").getAbsolutePath();
        assertThat(
                Elton4N.run(new String[]{
                        "compile",
                        "-datasetDir", getTestDatasetDir(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "-nameIndexCache", nameIndexCache,
                        "-neo4jVersion", neo4jVersion,
                        "link",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "-nameIndexCache", nameIndexCache,
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
                        "-nameIndexCache", nameIndexCache,
                        "-neo4jVersion", neo4jVersion
                }),
                Is.is(0)
        );

        File csvDir = new File(folder, "tsv");
        File interactions = new File(csvDir, "interactions.tsv.gz");
        FileInputStream is = new FileInputStream(interactions);
        String actualContent = IOUtils.toString(new GZIPInputStream(is), StandardCharsets.UTF_8);
        assertThat(actualContent, Is.is(IOUtils.toString(Elton4NTestUtil.class.getResourceAsStream(expectedOutput), StandardCharsets.UTF_8)));
    }

}