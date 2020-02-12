package org.globalbioticinteractions.cache;

import org.globalbioticinteractions.dataset.Dataset;

public interface CacheFactory {

    Cache cacheFor(Dataset dataset);

}
