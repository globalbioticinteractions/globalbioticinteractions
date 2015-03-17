package org.eol.globi.server.util;

import org.eol.globi.server.CypherTestUtil;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResultFormatterJSONLDTest {

    @Test
    public void formatSingleResult() throws IOException {
        String format = new ResultFormatterJSONLD().format(CypherTestUtil.CYPHER_RESULT);
        assertThat(format, is("{\"@context\": \"https://raw.githubusercontent.com/globalbioticinteractions/jsonld-template-dataset/master/context.jsonld\"}"));
    }
}

