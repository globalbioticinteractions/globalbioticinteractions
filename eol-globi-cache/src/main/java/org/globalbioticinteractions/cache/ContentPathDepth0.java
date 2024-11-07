package org.globalbioticinteractions.cache;

import java.io.File;
import java.net.URI;

/**
 * translates a content id (e.g., a hex-encoded sha256 hash) into some URI
 */
public class ContentPathDepth0 implements ContentPath {
    private final File cacheDir;

    public ContentPathDepth0(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    public URI forContentId(String contentId) {
        return new File(cacheDir, contentId).toURI();
    }
}
