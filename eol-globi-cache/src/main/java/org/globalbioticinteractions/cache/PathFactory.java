package org.globalbioticinteractions.cache;

import java.io.File;

public interface PathFactory<T> {
    T getPath(File cacheDir, String namespace);
}
