package org.eol.globi.server;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DietNicheWidthControllerStringTest extends SpringTestBase {

    @Autowired
    private DietNicheWidthController controller;

    @Test
    public void match() throws IOException {
        assertThat(controller, is(notNullValue()));
    }

}


