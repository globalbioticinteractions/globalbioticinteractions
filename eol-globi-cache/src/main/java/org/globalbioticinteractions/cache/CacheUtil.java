package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class CacheUtil {

    public static final String MIME_TYPE_GLOBI = "application/globi";
    public static final Logger LOG = LoggerFactory.getLogger(CacheUtil.class);

    public static Cache cacheFor(String namespace, String cacheDir, InputStreamFactory inputStreamFactory) {
        Cache pullThroughCache = new CachePullThrough(namespace, cacheDir, inputStreamFactory);
        CacheLocalReadonly readOnlyCache = new CacheLocalReadonly(namespace, cacheDir, inStream -> inStream);
        return new CacheProxy(Arrays.asList(readOnlyCache, pullThroughCache));
    }

    public static File findOrMakeCacheDirForNamespace(String cachePath, String namespace) throws IOException {
        File directory = findCacheDirForNamespace(cachePath, namespace);
        FileUtils.forceMkdir(directory);
        return directory;
    }

    public static File findCacheDirForNamespace(String cachePath, String namespace) {
        File cacheDir = new File(cachePath);
        return new File(cacheDir, namespace);
    }


    public static ContentProvenance cacheStream(InputStream inputStream, File cacheDir) throws IOException {
        File destinationFile = null;

        try (InputStream sourceStream = inputStream) {
            destinationFile = File.createTempFile("archive", "tmp", cacheDir);
            try {
                OutputStream os = FileUtils.openOutputStream(destinationFile);
                String sha256 = calculateContentHash(sourceStream, os);
                File destFile = new File(cacheDir, sha256);
                if (!destFile.exists()) {
                    FileUtils.moveFile(destinationFile, destFile);
                }
                return new ContentProvenance(null, null, destFile.toURI(), sha256, DateUtil.nowDateString());
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("failed to access hash/digest algorithm", e);
            }
        } finally {
            if (destinationFile != null && destinationFile.exists()) {
                FileUtils.deleteQuietly(destinationFile);
            }
        }
    }

    public static String calculateContentHash(InputStream sourceStream, OutputStream os) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (DigestInputStream digestInputStream = new DigestInputStream(sourceStream, md)) {
            IOUtils.copy(digestInputStream, os);
        }
        return String.format("%064x", new java.math.BigInteger(1, md.digest()));
    }

    public static ContentProvenance cache(URI sourceURI, File cacheDir, InputStreamFactory factory) throws IOException {
        String msg = "caching [" + sourceURI + "]";
        LOG.info(msg + " started...");
        InputStream inputStream = new ResourceServiceLocalAndRemote(factory).retrieve(sourceURI);
        ContentProvenance contentProvenance = cacheStream(inputStream, cacheDir);
        LOG.info(msg + " cached at [" + contentProvenance.getLocalURI().toString() + "]...");
        LOG.info(msg + " complete.");
        return contentProvenance;
    }

    public static boolean isLocalDir(URI archiveURI) {
        return archiveURI != null
                && StringUtils.equals("file", archiveURI.getScheme())
                && new File(archiveURI).exists()
                && new File(archiveURI).isDirectory();
    }

}
