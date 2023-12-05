package org.globalbioticinteractions.elton;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

public class Elton4NTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("target"));

    @Test
    public void compositeCommandV3() throws IOException, URISyntaxException {
        File newFolder = folder.newFolder();
        Elton4NTestUtil.assertCompileLinkExport(
                "3",
                newFolder,
                "/exported/interactions.tsv"
        );
    }

    @Test
    public void compositeCommandV2() throws IOException, URISyntaxException {
        File newFolder = folder.newFolder();
        Elton4NTestUtil.assertCompileLinkExport(
                "2",
                newFolder,
                "/exported/interactions2.tsv"
        );
    }

}