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

}
