package org.globalbioticinteractions.elton;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CmdCreateLocationIndexesTest  {

    @Test
    public void query() throws IOException {
        Path neo4j = Files.createTempDirectory("neo4j");
        try {
            CmdCreateLocationIndexes cmd = new CmdCreateLocationIndexes();
            cmd.setGraphDbDir(neo4j.toString());
            cmd.run();
            cmd.destroy();
        } finally {
            FileUtils.deleteQuietly(neo4j.toFile());
        }
    }


}