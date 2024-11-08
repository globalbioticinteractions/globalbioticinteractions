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
        String graphDb = new File(folder, "graph.db").getAbsolutePath();
        String export = new File(folder, "export").getAbsolutePath();
        assertThat(
                Elton4N.run(new String[]{
                        "compile",
                        "-datasetDir", getTestDatasetDir(),
                        "-provDir", getTestDatasetDir(),
                        "-graphDbDir", graphDb,
                        "-exportDir", export,
                        "-nameIndexCache", nameIndexCache,
                        "-neo4jVersion", neo4jVersion,
                        "link",
                        "-datasetDir", getTestDatasetDir(),
                        "-provDir", getTestDatasetDir(),
                        "-graphDbDir", graphDb,
                        "-exportDir", export,
                        "-nameIndexCache", nameIndexCache,
                        "-neo4jVersion", neo4jVersion,
                        "package",
                        "-datasetDir", getTestDatasetDir(),
                        "-provDir", getTestDatasetDir(),
                        "-graphDbDir", graphDb,
                        "-exportDir", export,
                        "-nameIndexCache", nameIndexCache,
                        "-neo4jVersion", neo4jVersion
                }),
                Is.is(0)
        );

        File csvDir = new File(export, "tsv");
        File interactions = new File(csvDir, "interactions.tsv.gz");
        FileInputStream is = new FileInputStream(interactions);
        String actualContent = IOUtils.toString(new GZIPInputStream(is), StandardCharsets.UTF_8);
        assertThat(actualContent, Is.is(IOUtils.toString(Elton4NTestUtil.class.getResourceAsStream(expectedOutput), StandardCharsets.UTF_8)));
    }

}