package org.eol.globi.tool;

import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.cache.CacheFactory;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryLocal;

public class DatasetRegistryUtil {

    public static DatasetRegistry getDatasetRegistry(String cacheDir, ResourceService resourceServiceLocal) {
        CacheFactory cacheFactory = dataset -> new CacheLocalReadonly(
                dataset.getNamespace(),
                cacheDir,
                resourceServiceLocal
        );
        return new DatasetRegistryLocal(cacheDir, cacheFactory, new ResourceServiceLocal(new InputStreamFactoryNoop()));
    }
}
