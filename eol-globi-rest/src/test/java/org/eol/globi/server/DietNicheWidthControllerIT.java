package org.eol.globi.server;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class DietNicheWidthControllerIT extends ITBase {

    @Test
    public void dietaryNicheAriopsis() throws IOException {
        String uri = getURLPrefix() + "dietNicheWidth/Ariopsis";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void dietaryNicheElasmobranchii() throws IOException {
        String uri = getURLPrefix() + "dietNicheWidth/Elasmobranchii";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(notNullValue()));
    }
}
