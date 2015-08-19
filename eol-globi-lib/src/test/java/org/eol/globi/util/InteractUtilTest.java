package org.eol.globi.util;

import org.eol.globi.domain.InteractType;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class InteractUtilTest {

    @Test
    public void interactionCypherClause() {
        assertThat(InteractUtil.interactionsCypherClause(new InteractType[]{
                InteractType.ATE, InteractType.SYMBIONT_OF}), is("ATE|SYMBIONT_OF"));
    }

}