package org.eol.globi.server;

import org.apache.http.client.HttpResponseException;
import org.eol.globi.util.HttpClient;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ImageServiceIT extends ITBase {

    @Test
    public void findImagesForExternalId() throws IOException {
        String uri = getURLPrefix() + "images/EOL:276287";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Oospila albicoma"));
    }

    @Test
    public void imagesForName() throws IOException {
        String uri = getURLPrefix() + "imagesForName/Homo%20sapiens";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void imagesForNames() throws IOException {
        String uri = getURLPrefix() + "imagesForNames?name=Homo%20sapiens&name=Ariopsis%20felis";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test(expected = HttpResponseException.class)
    public void imagesForNamesNoParams() throws IOException {
        String uri = getURLPrefix() + "imagesForNames";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(notNullValue()));
    }

}
