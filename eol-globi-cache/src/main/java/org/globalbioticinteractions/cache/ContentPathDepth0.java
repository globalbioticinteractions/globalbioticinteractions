package org.globalbioticinteractions.cache;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URI;

/**
 * translates a content id (e.g., a hex-encoded sha256 hash) into some URI
 */
public class ContentPathDepth0 implements ContentPath {
    private final File cacheDir;
    private final String namespace;

    public ContentPathDepth0(File cacheDir) {
        this(cacheDir, null);
    }

    public ContentPathDepth0(File cacheDir, String namespace) {
        this.cacheDir = cacheDir;
        this.namespace = namespace;
    }

    @Override
    public URI forContentId(String contentId) {

        return StringUtils.isBlank(namespace)
                ? new File(cacheDir, contentId).toURI()
                : CacheUtil.findCacheDirForNamespace(cacheDir, namespace).toURI();
    }
}
