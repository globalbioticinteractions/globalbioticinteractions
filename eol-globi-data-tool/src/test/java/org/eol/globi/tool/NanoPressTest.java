package org.eol.globi.tool;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class NanoPressTest {

    @Test
    public void init() throws IOException {
        List<String> ts = Arrays.asList("-i", "target/neo4j", "-o", "target/np");
        NanoPress.main(ts.toArray(new String[ts.size()]));
    }

}