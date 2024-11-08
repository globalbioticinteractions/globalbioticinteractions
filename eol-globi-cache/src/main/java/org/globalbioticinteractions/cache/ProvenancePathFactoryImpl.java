package org.globalbioticinteractions.cache;

import java.io.File;

public class ProvenancePathFactoryImpl implements ProvenancePathFactory {
    @Override
    public ProvenancePath getPath(File cacheDir, String namespace) {
        final ContentPathDepth0 contentPath1 = new ContentPathDepth0(cacheDir, namespace);
        return new ProvenancePathImpl(contentPath1);
    }

}
