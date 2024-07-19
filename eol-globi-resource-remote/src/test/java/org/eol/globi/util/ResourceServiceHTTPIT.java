package org.eol.globi.util;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class ResourceServiceHTTPIT {


    @Test
    public void retrieve() throws IOException {
        ResourceServiceHTTP service = new ResourceServiceHTTP(new InputStreamFactoryNoop());
        service.retrieve(URI.create("https://grlc.knowledgepixels.com/api-git/knowledgepixels/bdj-nanopub-api/get-taxontaxon-nanopubs.csv"));
    }

    @Test
    public void retrieveFromURLWithPathIncludingDoubleSlash() throws IOException {
        // see https://stackoverflow.com/questions/54591140/apache-http-client-stop-removing-double-slashes-from-url
        ResourceServiceHTTP service = new ResourceServiceHTTP(new InputStreamFactoryNoop());
        String doubleSlashInPath = "https://linker.bio/hash://sha256/69c839dc05a1b22d2e1aac1c84dec1cfd7af8425479053c74122e54998a1ddc2";
        service.retrieve(URI.create(doubleSlashInPath));
    }


}