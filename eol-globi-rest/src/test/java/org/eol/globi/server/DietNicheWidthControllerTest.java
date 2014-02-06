package org.eol.globi.server;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DietNicheWidthControllerTest {

    @Test
    public void dietaryNicheWidth() {
        assertThat(DietNicheWidthController.calculateDietaryNicheWidth(3, 2), is(0.5));
        assertThat(DietNicheWidthController.calculateDietaryNicheWidth(3, 1), is(0.0));
        assertThat(DietNicheWidthController.calculateDietaryNicheWidth(3, 3), is(1.0));
    }
}
