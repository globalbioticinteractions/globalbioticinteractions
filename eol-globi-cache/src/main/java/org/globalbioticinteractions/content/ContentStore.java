package org.globalbioticinteractions.content;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

public interface ContentStore {

    /**
     *
     * @param is stream of content to be stored
     * @return hash of stored content
     */

    public URI save(InputStream is) throws IOException;

    /**
     *
     * @param contentHash hash of requested content
     * @return stream of content with provided hash
     */

    public Optional<InputStream> retrieve(URI contentHash) throws IOException;

}
