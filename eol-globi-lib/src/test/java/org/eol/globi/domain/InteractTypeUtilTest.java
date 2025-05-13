package org.eol.globi.domain;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class InteractTypeUtilTest {

    @Test
    public void inverseOfRootParasiteOf() {
        InteractType interactType = InteractType.inverseOf(InteractType.ROOTPARASITE_OF);
        assertThat(interactType, Is.is(InteractType.HAS_PARASITE));
    }

    @Test
    public void inverseOfHemiParasiteOf() {
        InteractType interactType = InteractType.inverseOf(InteractType.HEMIPARASITE_OF);
        assertThat(interactType, Is.is(InteractType.HAS_PARASITE));
    }

    @Test
    public void inverseOfHasParasite() {
        InteractType interactType = InteractType.inverseOf(InteractType.HAS_PARASITE);
        assertThat(interactType, Is.is(InteractType.PARASITE_OF));
    }

}