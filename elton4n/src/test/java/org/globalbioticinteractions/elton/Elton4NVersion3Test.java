package org.globalbioticinteractions.elton;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

public class Elton4NVersion3Test {

    private File newFolder = null;

    @Before
    public void createDir() throws IOException {
        File target = new File("target");
        newFolder = new File(target, UUID.randomUUID().toString());
        newFolder.mkdir();
    }


    @Test
    public void compositeCommandV3() throws IOException, URISyntaxException {
        Elton4NTestUtil.assertCompileLinkExport(
                "3",
                newFolder,
                "/exported/interactions.tsv"
        );
    }

}