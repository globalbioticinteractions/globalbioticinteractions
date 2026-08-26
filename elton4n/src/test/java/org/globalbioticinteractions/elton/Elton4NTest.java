package org.globalbioticinteractions.elton;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

public class Elton4NTest {

    @Test
    public void runCompositeCommands() throws IOException, URISyntaxException {
        File tmpDir = new File(new File("target"), UUID.randomUUID().toString());
        tmpDir.deleteOnExit();
        FileUtils.forceMkdir(tmpDir);
        try {
            Elton4NTestUtil.assertCompileLinkExport(
                    tmpDir,
                    "/exported/interactions.tsv"
            );
        } finally {
            FileUtils.deleteQuietly(tmpDir);
        }
    }

}