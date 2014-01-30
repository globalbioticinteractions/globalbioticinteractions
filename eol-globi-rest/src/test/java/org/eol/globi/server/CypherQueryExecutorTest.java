package org.eol.globi.server;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class CypherQueryExecutorTest {

    @Test
    public void idTest() {
        assertThat(CypherQueryExecutor.getSafeLabel("EOL:123"), Is.is("EOL_123"));
        assertThat(CypherQueryExecutor.getSafeLabel("EOL//123"), Is.is("EOL__123"));
    }
}
