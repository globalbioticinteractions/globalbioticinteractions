package org.globalbioticinteractions.elton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class Elton4NTest {

    @Test
    public void runCompositeCommands() throws IOException, URISyntaxException {
        File tmpDir = new File(new File("target"), UUID.randomUUID().toString());
        tmpDir.deleteOnExit();
        FileUtils.forceMkdir(tmpDir);
        try {
            String nameIndexCache = new File(tmpDir, "nameIndexCache").getAbsolutePath();
            String graphDb = new File(tmpDir, "graph.db").getAbsolutePath();
            String export = new File(tmpDir, "export").getAbsolutePath();
            assertThat(
                    Elton4N.run(new String[]{
                            "compile",
                            "-datasetDir", Elton4NTestUtil.getTestDatasetDir(),
                            "-provDir", Elton4NTestUtil.getTestDatasetDir(),
                            "-graphDbDir", graphDb,
                            "link",
                            "-datasetDir", Elton4NTestUtil.getTestDatasetDir(),
                            "-provDir", Elton4NTestUtil.getTestDatasetDir(),
                            "-graphDbDir", graphDb,
                            "-exportDir", export,
                            "-nameIndexCache", nameIndexCache,
                            "-taxonCache", new File(Elton4NTestUtil.class.getResource("taxonCache.tsv").toURI()).getAbsolutePath(),
                            "-taxonMap", new File(Elton4NTestUtil.class.getResource("taxonMap.tsv").toURI()).getAbsolutePath(),
                            "package",
                            "-datasetDir", Elton4NTestUtil.getTestDatasetDir(),
                            "-provDir", Elton4NTestUtil.getTestDatasetDir(),
                            "-graphDbDir", graphDb,
                            "-exportDir", export,
                            "-nameIndexCache", nameIndexCache
                    }),
                    Is.is(0)
            );

            File csvDir = new File(export, "tsv");
            File interactions = new File(csvDir, "interactions.tsv.gz");
            FileInputStream is = new FileInputStream(interactions);
            String actualContent = IOUtils.toString(new GZIPInputStream(is), StandardCharsets.UTF_8);
            InputStream resourceAsStream = Elton4NTestUtil.class.getResourceAsStream("/exported/interactions.tsv");
            assertNotNull(resourceAsStream);
            String expected = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);

            assertThat(StringUtils.split(actualContent, "\n").length,
                    Is.is(StringUtils.split(expected, "\n").length));

            System.out.println("----");
            System.out.println(actualContent);

            System.out.println("----");
            System.out.println(expected);

            assertThat(actualContent, Is.is(expected));
        } finally {
            FileUtils.deleteQuietly(tmpDir);
        }
    }

}