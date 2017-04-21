package org.eol.globi.tool;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.db.GraphService;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class NanoPressTest {

    @Test
    public void init() throws IOException {
        FileUtils.deleteQuietly(new File("target/neo4j"));
        // instantiate node factory to create indexes
        new NodeFactoryNeo4j(GraphService.getGraphService("target/neo4j"));

        List<String> ts = Arrays.asList("-i", "target/neo4j", "-o", "target/np");
        NanoPress.main(ts.toArray(new String[ts.size()]));
    }

}