package org.globalbioticinteractions.cache;

import java.io.File;

public class ContentPathFactoryDepth0 implements ContentPathFactory {

    @Override
    public ContentPath getPath(File baseDir, String namespace) {
        return new ContentPathDepth0(baseDir, namespace);
    };
}
