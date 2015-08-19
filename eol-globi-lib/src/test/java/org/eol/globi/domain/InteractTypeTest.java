package org.eol.globi.domain;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class InteractTypeTest {

    @Test
    public void eolTerms() {
        assertThat(InteractType.typeOf("RO:0002459"), is(InteractType.VECTOR_OF));
        assertThat(InteractType.typeOf("http://eol.org/schema/terms/FlowersVisitedBy"), is(InteractType.FLOWERS_VISITED_BY));
        assertThat(InteractType.typeOf("http://eol.org/schema/terms/VisitsFlowersOf"), is(InteractType.VISITS_FLOWERS_OF));
    }

    @Test
    public void pathOf() {
        for (InteractType type : InteractType.values()) {
            assertThat("found missing path for interaction [" + type + "]", InteractType.pathOf(type), is(notNullValue()));
        }

        assertThat(InteractType.pathOf(InteractType.PREYS_UPON), hasItems(InteractType.KILLS, InteractType.ATE, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH));
        assertThat(InteractType.pathOf(InteractType.PREYED_UPON_BY), hasItems(InteractType.KILLED_BY, InteractType.EATEN_BY, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH));
    }

    @Test
    public void inverseOf() {
        for (InteractType type : InteractType.values()) {
            assertThat("found missing inverse for interaction [" + type + "]", InteractType.inverseOf(type), is(notNullValue()));
        }

        assertThat(InteractType.inverseOf(InteractType.PREYS_UPON), is(InteractType.PREYED_UPON_BY));
        assertThat(InteractType.inverseOf(InteractType.INTERACTS_WITH), is(InteractType.INTERACTS_WITH));
        assertThat(InteractType.inverseOf(InteractType.KILLS), is(InteractType.KILLED_BY));
        assertThat(InteractType.inverseOf(InteractType.ATE), is(InteractType.EATEN_BY));
    }

}