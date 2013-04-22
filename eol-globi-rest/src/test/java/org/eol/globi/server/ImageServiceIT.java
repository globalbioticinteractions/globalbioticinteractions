package org.eol.globi.server;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ImageServiceIT extends ITBase {

    @Test
    public void findImagesForExternalId() throws IOException {
        String uri = getURLPrefix() + "images/EOL:276287";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is("{\"description\":null,\"thumbnailURL\":\"http://media.eol.org/content/2011/12/13/21/66989_98_68.jpg\",\"imageURL\":\"http://media.eol.org/content/2011/12/13/21/66989_orig.jpg\",\"eolpageId\":\"276287\"}"));
    }

}
