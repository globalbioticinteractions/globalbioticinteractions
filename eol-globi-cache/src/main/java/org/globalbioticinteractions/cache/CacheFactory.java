package org.globalbioticinteractions.cache;

import org.globalbioticinteractions.dataset.Dataset;

import java.io.IOException;

public interface CacheFactory {

    Cache cacheFor(Dataset dataset) throws IOException;

}
