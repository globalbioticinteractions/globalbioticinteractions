package org.globalbioticinteractions.elton;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class Elton4NTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("target"));

    @Test
    public void runCompositeCommands() throws IOException, URISyntaxException {
        Elton4NTestUtil.assertCompileLinkExport(
                folder.newFolder(),
                "/exported/interactions.tsv"
        );
    }

}