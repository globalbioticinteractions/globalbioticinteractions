package org.globalbioticinteractions.elton;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

public class Elton4NTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void multipleCommands() throws IOException {
        assertThat(
                Elton4N.run(new String[]{
                        "compile",
                        "-datasetDir", folder.newFolder().getAbsolutePath(),
                        "link",
                        "package"}
                ),
                Is.is(0)
        );
    }


    @Test
    public void compositeCommand() throws IOException {
        assertThat(
                Elton4N.run(new String[]{
                        "compile",
                        "-datasetDir", folder.newFolder().getAbsolutePath(),
                        "link"}),
                Is.is(0)
        );

        assertThat(
                Elton4N.run(new String[]{"package"}),
                Is.is(0)
        );
    }

}