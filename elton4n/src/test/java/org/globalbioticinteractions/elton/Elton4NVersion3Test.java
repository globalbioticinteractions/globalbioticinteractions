package org.globalbioticinteractions.elton;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class Elton4NVersion3Test {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("target"));

    @Test
    public void compositeCommandV3() throws IOException, URISyntaxException {
        Elton4NTestUtil.assertCompileLinkExport(
                "3",
                folder.newFolder(),
                "/exported/interactions.tsv"
        );
    }

}