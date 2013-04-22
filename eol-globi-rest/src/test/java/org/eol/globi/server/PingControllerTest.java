package org.eol.globi.server;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class PingControllerTest extends SpringTestBase {

    @Autowired
    private PingController controller;

    @Test
    public void checkConfig() throws IOException {
        assertThat(controller, Is.is(notNullValue()));
    }

}
