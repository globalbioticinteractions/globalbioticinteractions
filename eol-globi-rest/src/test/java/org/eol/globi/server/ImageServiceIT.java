package org.eol.globi.server;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ImageServiceIT extends ITBase {

    @Test
    public void findImagesForExternalId() throws IOException {
        String uri = getURLPrefix() + "images/EOL:276287";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is("{\"commonName\":null,\"thumbnailURL\":\"http://media.eol.org/content/2011/12/13/21/66989_98_68.jpg\",\"imageURL\":\"http://media.eol.org/content/2011/12/13/21/66989_orig.jpg\",\"infoURL\":\"http://eol.org/pages/276287\",\"eolpageId\":\"276287\",\"scientificName\":\"Oospila albicoma\"}"));
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
