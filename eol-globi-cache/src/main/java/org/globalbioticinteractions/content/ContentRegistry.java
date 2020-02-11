package org.globalbioticinteractions.content;

import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

/**
 * Inspired by Document Registry actor in https://wiki.ihe.net/index.php/Cross-Enterprise_Document_Sharing .
 */

public interface ContentRegistry {

    /**
     * Register content of known provenance.
     *
     * @param contentProvenance provenance of content to be registered
     * @return provenance of content registered
     */

    ContentProvenance register(ContentProvenance contentProvenance) throws IOException;

    /**
     * Resolves to the provenance of registered content
     * produced by the requested content identifier
     * some point in time. Content identifiers include both
     * content hashes (e.g., hash://sha256/1234...) and
     * locators (e.g., https://example.org).
     *
     * Provenance includes time last accessed, original resource location
     * and associated content hash.
     *
     * @param knownContentIdentifier content identifier to be resolved
     * @return known locations and their provenance (e.g, source location, time accessed) that produced content with provided content hash
     */
    Stream<ContentProvenance> resolve(URI knownContentIdentifier);

}
