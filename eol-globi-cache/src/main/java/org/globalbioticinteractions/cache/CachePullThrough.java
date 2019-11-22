package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CachePullThrough implements Cache {
    private final static Log LOG = LogFactory.getLog(CachePullThrough.class);
    private final String namespace;
    private final String cachePath;
    private final InputStreamFactory inputStreamFactory;

    public CachePullThrough(String namespace, String cachePath) {
        this(namespace, cachePath, inStream -> inStream);
    }

    public CachePullThrough(String namespace, String cachePath, InputStreamFactory factory) {
        this.namespace = namespace;
        this.cachePath = cachePath;
        this.inputStreamFactory = factory;
    }

    static File cache(URI sourceURI, File cacheDir) throws IOException {
        return cache(sourceURI, cacheDir, inStream -> inStream);
    }

    static File cache(URI sourceURI, File cacheDir, InputStreamFactory factory) throws IOException {
        File destinationFile = null;
        try (InputStream sourceStream = ResourceUtil.asInputStream(sourceURI.toString(), factory)) {
            destinationFile = File.createTempFile("archive", "tmp", cacheDir);
            String msg = "caching [" + sourceURI + "]";
            LOG.info(msg + " started...");
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                try (DigestInputStream digestInputStream = new DigestInputStream(sourceStream, md)) {
                    FileUtils.copyInputStreamToFile(digestInputStream, destinationFile);
                }
                String sha256 = String.format("%064x", new java.math.BigInteger(1, md.digest()));
                File destFile = new File(cacheDir, sha256);
                if (destFile.exists()) {
                    LOG.info(msg + " nothing to do, [" + destFile.toURI().toString() + "] already cached...");
                } else {
                    FileUtils.moveFile(destinationFile, destFile);
                    LOG.info(msg + " cached at [" + destFile.toURI().toString() + "]...");
                }
                LOG.info(msg + " complete.");
                return destFile;
            } catch (NoSuchAlgorithmException e) {
                LOG.error("failed to access hash/digest algorithm", e);
                throw new IOException("failed to cache dataset [" + sourceURI.toString() + "]");
            }
        } finally {
            if (destinationFile != null && destinationFile.exists()) {
                FileUtils.deleteQuietly(destinationFile);
            }
        }
    }

    @Override
    public URI asURI(URI resourceURI) throws IOException {
        File cacheDir = CacheUtil.getCacheDirForNamespace(cachePath, namespace);
        File resourceCached = cache(resourceURI, cacheDir, getInputStreamFactory());
        CacheLog.appendCacheLog(namespace, resourceURI, cacheDir, resourceCached.toURI());
        return resourceCached.toURI();
    }

    @Override
    public CachedURI asMeta(URI resourceURI) {
        return null;
    }

    @Override
    public InputStream asInputStream(URI resourceURI) throws IOException {
        URI resourceURI1 = asURI(resourceURI);
        return resourceURI1 == null ? null : ResourceUtil.asInputStream(resourceURI1.toString(), getInputStreamFactory());
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }
}

