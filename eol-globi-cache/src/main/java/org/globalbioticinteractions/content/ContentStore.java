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
     * @param source stream of content to be stored and registered.
     * @return hash of stored content
     */

    ContentProvenance store(ContentSource source) throws IOException;

    /**
     *
     * @param contentLocator location of requested content
     * @return content source associated with  with provided hash
     */

    ContentSource retrieve(URI contentLocator) throws IOException;

}
