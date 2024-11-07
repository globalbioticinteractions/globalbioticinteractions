package org.globalbioticinteractions.cache;

import java.io.File;
import java.net.URI;

public class ContentPathFactory {
    private final File cacheDir;

    public ContentPathFactory(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public URI forContentId(String contentId) {
        return new File(cacheDir, contentId).toURI();
    }
}
