package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ResourceUtil;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Date;

public class CacheLocalReadonly implements Cache {
    private final Log LOG = LogFactory.getLog(CacheLocalReadonly.class);

    private final String namespace;
    private final String cachePath;

    public CacheLocalReadonly(String namespace, String cachePath) {
        this.namespace = namespace;
        this.cachePath = cachePath;
    }

    static URI getRemoteJarURIIfNeeded(URI remoteArchiveURI, URI localResourceURI) {
        URI remoteResourceURI = localResourceURI;
        if (isJarResource(localResourceURI) && !isJarResource(remoteArchiveURI)) {
            URI datasetArchiveURI = getDatasetArchiveURI(localResourceURI);
            remoteResourceURI = URI.create(StringUtils.replace(localResourceURI.toString(), datasetArchiveURI.toString(), remoteArchiveURI.toString()));
        }
        return remoteResourceURI;
    }

    @Override
    public URI asURI(URI resourceURI) throws IOException {
        CachedURI cachedUri = asMeta(resourceURI);
        return cachedUri == null ? null : cachedUri.getCachedURI();
    }

    @Override
    public CachedURI asMeta(URI resourceURI) {
        CachedURI meta = null;
        File accessFile;
        try {
            File cacheDirForNamespace = CacheUtil.getCacheDirForNamespace(cachePath, namespace);

            String hashCandidate = getHashCandidate(resourceURI, cacheDirForNamespace.toURI());
            accessFile = CacheLog.getAccessFile(cacheDirForNamespace);
            if (accessFile.exists()) {
                String[] rows = IOUtils.toString(accessFile.toURI()).split("\n");
                for (String row : rows) {
                    String[] split = CSVTSVUtil.splitTSV(row);
                    if (split.length > 3) {
                        URI sourceURI = URI.create(split[1]);
                        String sha256 = split[2];
                        String accessedAt = StringUtils.trim(split[3]);
                        if (StringUtils.isNotBlank(sha256)) {
                            CachedURI metaURI = getMetaURI(resourceURI, hashCandidate, sourceURI, sha256, accessedAt, cacheDirForNamespace);
                            meta = metaURI == null ? meta : metaURI;
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("unexpected exception on getting meta for [" + resourceURI + "]", e);
        }
        return meta;

    }

    public CachedURI getMetaURI(URI resourceURI, String localArchiveSha256, URI sourceURI, String sha256, String accessedAt, File cacheDir) {
        CachedURI meta = null;
        if (inCachedArchive(localArchiveSha256, sha256)) {
            meta = new CachedURI(namespace, getRemoteJarURIIfNeeded(sourceURI, resourceURI), resourceURI, sha256, accessedAt);
        } else if ((StringUtils.equals(resourceURI.toString(), sourceURI.toString())
                && !inCachedArchive(localArchiveSha256, sha256))) {
            URI localResourceURI = new File(cacheDir, sha256).toURI();
            meta = new CachedURI(namespace, sourceURI, localResourceURI, sha256, accessedAt);
        }
        return meta;
    }

    public boolean inCachedArchive(String localArchiveSha256, String sha256) {
        return StringUtils.isNotBlank(localArchiveSha256) && StringUtils.equals(localArchiveSha256, sha256);
    }

    static String getHashCandidate(URI resourceURI, URI cacheDir) {
        String hashCandidate = null;
        URI candidateURI = resourceURI;
        candidateURI = getDatasetArchiveURI(candidateURI);

        if (StringUtils.startsWith(candidateURI.toString(), cacheDir.toString())) {
            hashCandidate = StringUtils.replace(candidateURI.toString(), cacheDir.toString(), "");
        }
        return hashCandidate;
    }

    static URI getDatasetArchiveURI(URI candidateURI) {
        if (isJarResource(candidateURI)) {
            URLConnection urlConnection;
            try {
                urlConnection = candidateURI.toURL().openConnection();
                if (urlConnection instanceof JarURLConnection) {
                    candidateURI = ((JarURLConnection) urlConnection).getJarFileURL().toURI();
                    IOUtils.closeQuietly(urlConnection.getInputStream());
                }
            } catch (IOException | URISyntaxException e) {
                // ignore
            }
        }
        return candidateURI;
    }

    static boolean isJarResource(URI candidateURI) {
        return candidateURI != null && "jar".equals(candidateURI.getScheme());
    }

    @Override
    public InputStream asInputStream(URI resourceURI) throws IOException {
        URI resourceURI1 = asURI(resourceURI);
        return resourceURI1 == null ? null : ResourceUtil.asInputStream(resourceURI1.toString());
    }
}

