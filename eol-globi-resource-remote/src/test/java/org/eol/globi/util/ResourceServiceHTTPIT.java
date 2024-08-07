package org.eol.globi.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;

public class ResourceServiceHTTPIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void retrieve() throws IOException {
        ResourceServiceHTTP service = new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
        service.retrieve(URI.create("https://grlc.knowledgepixels.com/api-git/knowledgepixels/bdj-nanopub-api/get-taxontaxon-nanopubs.csv"));
    }

    @Test
    public void retrieveFromURLWithPathIncludingDoubleSlash() throws IOException {
        // see https://stackoverflow.com/questions/54591140/apache-http-client-stop-removing-double-slashes-from-url
        ResourceServiceHTTP service = new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
        String doubleSlashInPath = "https://linker.bio/hash://sha256/69c839dc05a1b22d2e1aac1c84dec1cfd7af8425479053c74122e54998a1ddc2";
        service.retrieve(URI.create(doubleSlashInPath));
    }

    @Test
    public void cachedRemote() throws IOException {
        String doubleSlashInPath = "https://linker.bio/hash://sha256/69c839dc05a1b22d2e1aac1c84dec1cfd7af8425479053c74122e54998a1ddc2";
        ResourceServiceHTTP.getCachedRemoteInputStream(URI.create(doubleSlashInPath), is -> is);
    }


}