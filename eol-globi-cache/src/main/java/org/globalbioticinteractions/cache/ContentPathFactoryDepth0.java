package org.globalbioticinteractions.cache;

import java.io.File;

public class ContentPathFactoryDepth0 implements ContentPathFactory {

    public ContentPath getContentPath(File baseDir) {
        return new ContentPathDepth0(baseDir);
    };
}