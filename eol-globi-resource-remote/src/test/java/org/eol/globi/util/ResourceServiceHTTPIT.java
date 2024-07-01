package org.eol.globi.util;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;

public class ResourceServiceHTTPIT {


    @Test
    public void retrieve() throws IOException {
        ResourceServiceHTTP service = new ResourceServiceHTTP(new InputStreamFactoryNoop());
        service.retrieve(URI.create("https://grlc.knowledgepixels.com/api-git/knowledgepixels/bdj-nanopub-api/get-taxontaxon-nanopubs.csv"));
    }

}