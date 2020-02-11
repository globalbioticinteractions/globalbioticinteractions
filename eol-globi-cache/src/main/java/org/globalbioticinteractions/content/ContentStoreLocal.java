package org.globalbioticinteractions.content;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class ContentStoreLocal implements ContentStore {

    public static final String HASH_SHA256_PREFIX = "hash://sha256/";
    private final File storeDir;
    private final String namespace;
    private final ContentRegistry contentRegistry;
    private final InputStreamFactory inputStreamFactory;

    public ContentStoreLocal(File storeDir, String namespace, InputStreamFactory inputStreamFactory, ContentRegistry contentRegistry) {
        this.storeDir = storeDir;
        this.namespace = namespace;
        this.contentRegistry = contentRegistry;
        this.inputStreamFactory = inputStreamFactory;
    }

    /**
     *
     * @param is stream of content to be stored
     * @return provenance of the provided and registered content
     * @throws IOException
     */

    @Override
    public ContentProvenance provideAndRegister(InputStream is) throws IOException {
        URI generatedContentURI = URI.create(UUID.randomUUID().toString());
        File cacheDirForNamespace = CacheUtil.getCacheDirForNamespace(storeDir.getAbsolutePath(), namespace);
        ContentProvenance localProvenance = CacheUtil.cacheStream(is, cacheDirForNamespace);
        ContentProvenance provenanceInNamespace = new ContentProvenance(namespace, generatedContentURI, localProvenance.getLocalURI(), localProvenance.getSha256(), localProvenance.getSha256());
        return contentRegistry.register(provenanceInNamespace);
    }

    @Override
    public ContentProvenance provideAndRegister(URI contentLocationURI) throws IOException {
        File cacheDir = CacheUtil.getCacheDirForNamespace(storeDir.getAbsolutePath(), namespace);
        ContentProvenance localResourceLocation = CacheUtil.cache(contentLocationURI, cacheDir, getInputStreamFactory());
        ContentProvenance contentProvenanceWithNamespace = new ContentProvenance(namespace, contentLocationURI, localResourceLocation.getLocalURI(), localResourceLocation.getSha256(), localResourceLocation.getAccessedAt());
        return contentRegistry.register(contentProvenanceWithNamespace);
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }

    @Override
    public Optional<InputStream> retrieve(URI contentHash) throws IOException {
        File cacheDir = CacheUtil.getCacheDirForNamespace(storeDir.getAbsolutePath(), namespace);
        File localFile = null;
        if (StringUtils.startsWith(contentHash.toString(), HASH_SHA256_PREFIX)) {
            URI localResourceURI = new File(cacheDir, StringUtils.substring(contentHash.toString(), HASH_SHA256_PREFIX.length())).toURI();
            localFile = new File(localResourceURI);
        }
        return localFile == null || !localFile.exists()
                ? Optional.empty()
                : Optional.of(new FileInputStream(localFile));
    }
}
