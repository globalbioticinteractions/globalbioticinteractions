package org.globalbioticinteractions.cache;

import java.io.File;

public interface ProvenancePathFactory {
    ProvenancePath getProvenancePath(File cacheDirForNamespace);
}
