package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.util.OpenBiodivClient;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SparqlClientCachingFactory extends SparqlClientOpenBiodivFactory {

    private AtomicReference<DB> db = new AtomicReference<>();

    @Override
    public SparqlClient create(ResourceService resourceService) {
        DB db = getDb();
        final Map<String, String> queryCache = db.createTreeMap("queryCache").make();
        final ResourceService resourceServiceCaching = new ResourceServiceCaching(queryCache, resourceService);
        return new OpenBiodivClient(resourceServiceCaching) {
            @Override
            public void close() throws IOException {
                db.close();
            }
        };
    }

    public DB getDb() {
        if (db.get() == null) {
            db.set(DBMaker
                    .newMemoryDirectDB()
                    .compressionEnable()
                    .transactionDisable()
                    .make());
        }
        return db.get();
    }

    private static class ResourceServiceCaching implements ResourceService {
        private final Map<String, String> queryCache;
        private final ResourceService resourceService;

        public ResourceServiceCaching(Map<String, String> queryCache, ResourceService resourceService) {
            this.queryCache = queryCache;
            this.resourceService = resourceService;
        }

        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            final String resourceNameString = resourceName.toString();
            String result = queryCache.get(resourceNameString);
            if (result == null) {
                result = IOUtils.toString(resourceService.retrieve(resourceName), StandardCharsets.UTF_8);
                queryCache.put(resourceNameString, result);
            }
            return IOUtils.toInputStream(result, StandardCharsets.UTF_8);
        }
    }
}