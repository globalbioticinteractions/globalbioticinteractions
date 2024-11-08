package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static Cache cacheFor(String namespace,
                                 String cacheDir,
                                 ResourceService resourceServiceRemote,
                                 ResourceService resourceServiceLocal,
                                 ContentPathFactory contentPathFactory,
                                 ProvenancePathFactory provenancePathFactory,
                                 String provPath) {
        Cache pullThroughCache = new CachePullThrough(namespace, cacheDir, resourceServiceRemote, contentPathFactory);
        CacheLocalReadonly readOnlyCache = new CacheLocalReadonly(namespace, cacheDir, resourceServiceLocal, contentPathFactory, provenancePathFactory, provPath);
        return new CacheProxy(Arrays.asList(readOnlyCache, pullThroughCache));
    }

    public static File findOrMakeCacheDirForNamespace(String cachePath, String namespace) throws IOException {
        File directory = findCacheDirForNamespace(cachePath, namespace);
        FileUtils.forceMkdir(directory);
        return directory;
    }

    public static File findOrMakeCacheDirForNamespace(File cachePath, String namespace) throws IOException {
        File directory = findCacheDirForNamespace(cachePath, namespace);
        FileUtils.forceMkdir(directory);
        return directory;
    }

    public static File findCacheDirForNamespace(String cachePath, String namespace) {
        return findCacheDirForNamespace(new File(cachePath), namespace);
    }

    public static File findCacheDirForNamespace(File cachePath, String namespace) {
        return new File(cachePath, namespace);
    }


    public static ContentProvenance cacheStream(
            InputStream inputStream,
            File cacheDir,
            ContentPathFactory contentPathFactory,
            String namespace) throws IOException {
        File destinationFile = null;

        File cacheDirForNamespace = CacheUtil.findOrMakeCacheDirForNamespace(cacheDir, namespace);

        try (InputStream sourceStream = inputStream) {
            destinationFile = File.createTempFile("archive", "tmp", cacheDirForNamespace);
            try {
                OutputStream os = FileUtils.openOutputStream(destinationFile);
                String sha256 = calculateContentHash(sourceStream, os);
                URI uri = contentPathFactory.getPath(cacheDir, namespace).forContentId(sha256);
                File destFile = new File(uri);
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

    public static ContentProvenance cache(URI sourceURI,
                                          File cacheDir,
                                          ResourceService resourceService,
                                          ContentPathFactory contentPathFactory,
                                          String namespace) throws IOException {
        String msg = "caching [" + sourceURI + "]";
        LOG.info(msg + " started...");
        InputStream inputStream = resourceService.retrieve(sourceURI);
        ContentProvenance contentProvenance = cacheStream(inputStream, cacheDir, contentPathFactory, namespace);
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
