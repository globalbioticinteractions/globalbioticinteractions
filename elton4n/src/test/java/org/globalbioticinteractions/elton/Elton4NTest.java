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

    @Test
    public void queryDefault() throws IOException {
        Elton4N.main(new String[]{"query", "-graphDbDir", folder.newFolder("neo4j").getAbsolutePath()});
    }

    @Test
    public void queryCustom() throws IOException {
        Elton4N.main(new String[]{"query", "-c", "MATCH(n) RETURN n LIMIT 1;", "-graphDbDir", folder.newFolder("neo4j").getAbsolutePath()});
    }

}