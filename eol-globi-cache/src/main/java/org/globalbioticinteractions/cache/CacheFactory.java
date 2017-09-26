package org.globalbioticinteractions.cache;

import org.eol.globi.service.Dataset;

public interface CacheFactory {

    Cache cacheFor(Dataset dataset);

}
