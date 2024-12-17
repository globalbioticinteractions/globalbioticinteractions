package org.globalbioticinteractions.cache;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URI;

/**
 * translates a content id (e.g., a hex-encoded sha256 hash) into some URI
 */
public class ContentPathDepth2 implements ContentPath {
    private final File cacheDir;
    private final String namespace;

    public ContentPathDepth2(File cacheDir) {
        this(cacheDir, null);
    }

    public ContentPathDepth2(File cacheDir, String namespace) {
        this.cacheDir = cacheDir;
        this.namespace = namespace;
    }

    @Override
    public URI forContentId(String contentId) {

        String firstTwo = StringUtils.substring(contentId, 0, 2);
        String secondTwo = StringUtils.substring(contentId, 2, 4);

        File cacheDirWithNamespace = StringUtils.isBlank(namespace)
                ? this.cacheDir
                : CacheUtil.findCacheDirForNamespace(this.cacheDir, namespace);

        File first = new File(cacheDirWithNamespace, firstTwo);
        File second = new File(first, secondTwo);
        return new File(second, contentId).toURI();
    }
}
