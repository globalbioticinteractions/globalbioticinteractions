package org.globalbioticinteractions.content;

import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 *  Inspired by Document Store actor in https://wiki.ihe.net/index.php/Cross-Enterprise_Document_Sharing .
 */

public interface ContentStore {

    /**
     *  Stores provided content, records the provenance and registers it.
     *
     * @param is stream of content to be stored and registered.
     * @return hash of stored content
     */

    ContentProvenance store(InputStream is) throws IOException;

    ContentProvenance store(URI contentLocationURI) throws IOException;

    /**
     *
     * @param contentHash hash of requested content
     * @return stream of content with provided hash
     */

    ContentSource retrieve(URI contentHash) throws IOException;

}
