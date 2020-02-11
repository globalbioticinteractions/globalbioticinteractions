package org.globalbioticinteractions.content;

import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

public interface ContentRegistry {

    /**
     *
     * @param contentLocationURI location of content to be registered
     * @return hash of content produced by the provided URI
     */

    public URI register(URI contentLocationURI) throws IOException;

    /**
     * Resolves to the provenance of registered content locations
     * that produced content with the requested content hash at
     * some point in time.
     *
     * Provenance includes time last accessed, original resource location
     * and associated content hash.
     *
     * @param contentHash hash of registered content
     * @return known locations and their provenance (e.g, source location, time accessed) that produced content with provided content hash
     */
    public Stream<ContentProvenance> resolve(URI contentHash);

}
