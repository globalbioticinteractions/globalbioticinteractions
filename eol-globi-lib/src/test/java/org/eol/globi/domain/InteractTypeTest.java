package org.eol.globi.domain;

import org.junit.Test;

import static org.eol.globi.domain.InteractType.ATE;
import static org.eol.globi.domain.InteractType.DAMAGED_BY;
import static org.eol.globi.domain.InteractType.DAMAGES;
import static org.eol.globi.domain.InteractType.DISPERSAL_VECTOR_OF;
import static org.eol.globi.domain.InteractType.EATEN_BY;
import static org.eol.globi.domain.InteractType.ECTOPARASITE_OF;
import static org.eol.globi.domain.InteractType.ENDOPARASITE_OF;
import static org.eol.globi.domain.InteractType.FARMED_BY;
import static org.eol.globi.domain.InteractType.FARMS;
import static org.eol.globi.domain.InteractType.FLOWERS_VISITED_BY;
import static org.eol.globi.domain.InteractType.HAS_DISPERAL_VECTOR;
import static org.eol.globi.domain.InteractType.HAS_ECTOPARASITE;
import static org.eol.globi.domain.InteractType.HAS_ENDOPARASITE;
import static org.eol.globi.domain.InteractType.HAS_HYPERPARASITE;
import static org.eol.globi.domain.InteractType.HAS_HYPERPARASITOID;
import static org.eol.globi.domain.InteractType.HAS_PARASITE;
import static org.eol.globi.domain.InteractType.HAS_PARASITOID;
import static org.eol.globi.domain.InteractType.HAS_PATHOGEN;
import static org.eol.globi.domain.InteractType.HAS_VECTOR;
import static org.eol.globi.domain.InteractType.HOST_OF;
import static org.eol.globi.domain.InteractType.HYPERPARASITE_OF;
import static org.eol.globi.domain.InteractType.HYPERPARASITOID_OF;
import static org.eol.globi.domain.InteractType.INTERACTS_WITH;
import static org.eol.globi.domain.InteractType.KILLED_BY;
import static org.eol.globi.domain.InteractType.KILLS;
import static org.eol.globi.domain.InteractType.PARASITE_OF;
import static org.eol.globi.domain.InteractType.PARASITOID_OF;
import static org.eol.globi.domain.InteractType.PATHOGEN_OF;
import static org.eol.globi.domain.InteractType.POLLINATED_BY;
import static org.eol.globi.domain.InteractType.POLLINATES;
import static org.eol.globi.domain.InteractType.PREYED_UPON_BY;
import static org.eol.globi.domain.InteractType.PREYS_UPON;
import static org.eol.globi.domain.InteractType.SYMBIONT_OF;
import static org.eol.globi.domain.InteractType.VECTOR_OF;
import static org.eol.globi.domain.InteractType.VISITS_FLOWERS_OF;
import static org.eol.globi.domain.InteractType.typeOf;
import static org.eol.globi.domain.InteractType.values;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class InteractTypeTest {

    @Test
    public void eolTerms() {
        assertThat(typeOf("RO:0002459"), is(VECTOR_OF));
        assertThat(typeOf("http://eol.org/schema/terms/FlowersVisitedBy"), is(FLOWERS_VISITED_BY));
        assertThat(typeOf("http://eol.org/schema/terms/VisitsFlowersOf"), is(VISITS_FLOWERS_OF));
    }

    @Test
    public void hasTypes() {
        for (InteractType type : values()) {
            assertThat("found missing path for interaction [" + type + "]", InteractType.hasTypes(type), is(notNullValue()));
        }

        assertThat(InteractType.hasTypes(PREYS_UPON), hasItems(KILLS, ATE, SYMBIONT_OF, INTERACTS_WITH));
        assertThat(InteractType.hasTypes(PREYED_UPON_BY), hasItems(KILLED_BY, EATEN_BY, SYMBIONT_OF, INTERACTS_WITH));
        assertThat(InteractType.hasTypes(DAMAGED_BY), hasItems(SYMBIONT_OF, INTERACTS_WITH));
    }

    @Test
    public void typesOf() {
        for (InteractType type : values()) {
            assertThat("found missing path for interaction [" + type + "]", InteractType.typesOf(type), is(notNullValue()));
        }

        assertThat(InteractType.typesOf(KILLS), hasItems(PREYS_UPON, HYPERPARASITOID_OF, PARASITOID_OF));
        assertThat(InteractType.typesOf(SYMBIONT_OF), hasItems(PREYS_UPON, PARASITE_OF, POLLINATES, ATE, PREYED_UPON_BY, POLLINATED_BY, EATEN_BY, HAS_PARASITE, HAS_PATHOGEN, PATHOGEN_OF, HAS_VECTOR, VECTOR_OF, ENDOPARASITE_OF, HAS_ENDOPARASITE, HYPERPARASITE_OF, HAS_HYPERPARASITE, HYPERPARASITOID_OF, HAS_HYPERPARASITOID, ECTOPARASITE_OF, HAS_ECTOPARASITE, PARASITOID_OF, HAS_PARASITOID, FARMED_BY, FARMS, DAMAGED_BY, DAMAGES, DISPERSAL_VECTOR_OF, HAS_DISPERAL_VECTOR, KILLED_BY, KILLS));
        assertThat(InteractType.typesOf(PARASITE_OF), hasItems(PATHOGEN_OF, ENDOPARASITE_OF, HYPERPARASITE_OF, HYPERPARASITOID_OF, ECTOPARASITE_OF, PARASITOID_OF));
        assertThat(InteractType.typesOf(PARASITE_OF), not(hasItem(HOST_OF)));
    }

    @Test
    public void inverseOf() {
        for (InteractType type : values()) {
            assertThat("found missing inverse for interaction [" + type + "]", InteractType.inverseOf(type), is(notNullValue()));
        }

        assertThat(InteractType.inverseOf(PREYS_UPON), is(PREYED_UPON_BY));
        assertThat(InteractType.inverseOf(INTERACTS_WITH), is(INTERACTS_WITH));
        assertThat(InteractType.inverseOf(KILLS), is(KILLED_BY));
        assertThat(InteractType.inverseOf(ATE), is(EATEN_BY));
    }

}