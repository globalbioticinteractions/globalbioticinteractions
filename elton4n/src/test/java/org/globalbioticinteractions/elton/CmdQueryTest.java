package org.globalbioticinteractions.elton;


import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.QueryExecutionException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CmdQueryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void query() throws IOException {
        CmdQuery cmd = new CmdQuery();
        cmd.setGraphDbDir(folder.newFolder("neo4j").getAbsolutePath());
        cmd.setStdin(IOUtils.toInputStream("MATCH (n) RETURN n LIMIT 1;", StandardCharsets.UTF_8));
        cmd.run();
    }

    @Test(expected = QueryExecutionException.class)
    public void invalidQuery() throws IOException {
        CmdQuery cmd = new CmdQuery();
        cmd.setGraphDbDir(folder.newFolder("neo4j").getAbsolutePath());
        cmd.setStdin(IOUtils.toInputStream("why did the chicken cross the street?", StandardCharsets.UTF_8));
        cmd.run();
    }

}