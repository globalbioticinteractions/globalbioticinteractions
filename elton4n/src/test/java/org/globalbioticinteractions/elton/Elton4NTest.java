package org.globalbioticinteractions.elton;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

public class Elton4NTest {

    public File folder;

    @Before
    public void createFolder() throws IOException {
        folder = new File("target", "elton4n-test");
        FileUtils.forceMkdir(folder);
    }

    @After
    public void deleteFolder() throws IOException {
        if (folder != null) {
            FileUtils.forceDelete(folder);
        }
    }

    @Test
    public void compositeCommand() throws IOException {
        assertThat(
                Elton4N.run(new String[]{
                        "compile",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "link",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath()
                }),
                Is.is(0)
        );

        assertThat(
                Elton4N.run(new String[]{
                        "package",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath()
                }),
                Is.is(0)
        );
    }

    @Test
    public void compositeCommandV3() throws IOException {
        assertThat(
                Elton4N.run(new String[]{
                        "compile",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "-neo4jVersion", "3",
                        "link",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "-neo4jVersion", "3",
                }),
                Is.is(0)
        );

        assertThat(
                Elton4N.run(new String[]{
                        "package",
                        "-datasetDir", folder.getAbsolutePath(),
                        "-graphDbDir", folder.getAbsolutePath(),
                        "-exportDir", folder.getAbsolutePath(),
                        "-neo4jVersion", "3"
                }),
                Is.is(0)
        );
    }

}