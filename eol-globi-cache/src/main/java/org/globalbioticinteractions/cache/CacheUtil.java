package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.DateUtil;
import org.globalbioticinteractions.dataset.HashCalculator;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CacheUtil {

    public static final String MIME_TYPE_GLOBI = "application/globi";
    public static final Logger LOG = LoggerFactory.getLogger(CacheUtil.class);
    public static final Pattern FILE_URL_PATTERN = Pattern.compile("([a-z]+:)*(file:)(?<filepath>[^!]+)(.*)");

    public static Cache cacheFor(String namespace,
                                 String dataDir,
                                 String provDir,
                                 ResourceService resourceServiceRemote,
                                 ResourceService resourceServiceLocal,
                                 ContentPathFactory contentPathFactory,
                                 ProvenancePathFactory provenancePathFactory,
                                 HashCalculator hashCalculator) {
        Cache pullThroughCache = new CachePullThrough(namespace, resourceServiceRemote, contentPathFactory, dataDir, provDir, hashCalculator);
        CacheLocalReadonly readOnlyCache = new CacheLocalReadonly(namespace, dataDir, provDir, resourceServiceLocal, contentPathFactory, provenancePathFactory);
        return new CacheProxy(Arrays.asList(readOnlyCache, pullThroughCache));
    }

    public static File findOrMakeProvOrDataDirForNamespace(String cachePath, String namespace) throws IOException {
        return findOrMakeProvOrDataDirForNamespace(new File(cachePath), namespace);
    }

    public static File findOrMakeProvOrDataDirForNamespace(File cachePath, String namespace) throws IOException {
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
            String namespace,
            HashCalculator hashCalculator) throws IOException {
        File destinationFile = null;

        File cacheDirForNamespace = CacheUtil.findOrMakeProvOrDataDirForNamespace(cacheDir, namespace);

        try (InputStream sourceStream = inputStream) {
            destinationFile = File.createTempFile("archive", "tmp", cacheDirForNamespace);
            try {
                OutputStream os = FileUtils.openOutputStream(destinationFile);
                String sha256 = hashCalculator.calculateContentHash(sourceStream, NullOutputStream.NULL_OUTPUT_STREAM);
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
                                          File dataDir,
                                          ResourceService resourceService,
                                          ContentPathFactory contentPathFactory,
                                          String namespace,
                                          HashCalculator hashCalculator) throws IOException {
        String msg = "caching [" + sourceURI + "]";
        LOG.info(msg + " started...");
        InputStream inputStream = resourceService.retrieve(sourceURI);
        ContentProvenance contentProvenance = cacheStream(inputStream, dataDir, contentPathFactory, namespace, hashCalculator);
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

    public static boolean isInCacheDir(File cacheDir, URI resource) {
        boolean isInCacheDir = false;
        if (cacheDir != null && resource != null) {
            Matcher matcher = FILE_URL_PATTERN.matcher(resource.toString());
            if (matcher.matches()) {
                String filepath = matcher.group("filepath");
                String filepathResource = new File(URI.create("file:" + filepath)).getAbsolutePath();
                isInCacheDir = StringUtils.startsWith(filepathResource, cacheDir.getAbsolutePath());
            }
        }
        return isInCacheDir;
    }

}
