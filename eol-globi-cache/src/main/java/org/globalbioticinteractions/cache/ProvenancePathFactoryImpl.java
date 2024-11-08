package org.globalbioticinteractions.cache;

import java.io.File;

public class ProvenancePathFactoryImpl implements ProvenancePathFactory {
    @Override
    public ProvenancePath getProvenancePath(File cacheDirForNamespace) {
        final ContentPathDepth0 contentPath1 = new ContentPathDepth0(cacheDirForNamespace);
        return new ProvenancePathImpl(contentPath1);
    }

}
