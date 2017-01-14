package org.eol.globi.util;

import org.eol.globi.domain.InteractType;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class InteractUtilTest {

    @Test
    public void interactionCypherClause() {
        String value = "ATE|PREYS_UPON|PARASITE_OF|ENDOPARASITE_OF|HYPERPARASITE_OF|HYPERPARASITOID_OF|ECTOPARASITE_OF|KLEPTOPARASITE_OF|PARASITOID_OF|ENDOPARASITOID_OF|ECTOPARASITOID_OF|FARMS|SYMBIONT_OF|PARASITE_OF|HAS_HOST|HOST_OF|POLLINATES|POLLINATED_BY|HAS_PARASITE|HAS_PATHOGEN|PATHOGEN_OF|HAS_VECTOR|VECTOR_OF|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|HYPERPARASITOID_OF|HAS_HYPERPARASITOID|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|FARMED_BY|FARMS|EPIPHITE_OF|HAS_EPIPHITE";
        assertThat(InteractUtil.interactionsCypherClause(InteractType.ATE, InteractType.SYMBIONT_OF)
                , is(value));
    }

    @Test
    public void interactionCypherClause2() {
        assertThat(InteractUtil.interactionsCypherClause(InteractType.VECTOR_OF)
                , is("VECTOR_OF|DISPERSAL_VECTOR_OF"));
    }

}