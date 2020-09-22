package org.eol.globi.tool;

import org.globalbioticinteractions.cache.CacheFactory;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryLocal;

public class DatasetRegistryUtil {

    public static DatasetRegistry getDatasetRegistry(String cacheDir) {
        CacheFactory cacheFactory = dataset -> new CacheLocalReadonly(dataset.getNamespace(), cacheDir);
        return new DatasetRegistryLocal(cacheDir, cacheFactory);
    }
}
