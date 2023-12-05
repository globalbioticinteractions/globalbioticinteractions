package org.globalbioticinteractions.elton;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class Elton4NVersion2Test {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("target"));

    @Test
    public void compositeCommand() throws IOException, URISyntaxException {
        Elton4NTestUtil.assertCompileLinkExport(
                "2",
                folder.newFolder(),
                "/exported/interactions2.tsv"
        );
    }


}