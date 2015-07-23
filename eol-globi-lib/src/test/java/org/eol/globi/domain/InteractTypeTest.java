package org.eol.globi.domain;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class InteractTypeTest {

    @Test
    public void eolTerms() {
        assertThat(InteractType.typeOf("RO:0002459"), is(InteractType.VECTOR_OF));
        assertThat(InteractType.typeOf("http://eol.org/schema/terms/FlowersVisitedBy"), is(InteractType.FLOWERS_VISITED_BY));
        assertThat(InteractType.typeOf("http://eol.org/schema/terms/VisitsFlowersOf"), is(InteractType.VISITS_FLOWERS_OF));
    }

}