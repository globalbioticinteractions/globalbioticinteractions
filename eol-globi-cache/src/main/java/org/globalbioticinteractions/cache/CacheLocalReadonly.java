package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
        if (isJarResource(localResourceURI)) {
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
                    String[] split = row.split("\t");
                    if (split.length > 3) {
                        String hash256 = split[2];
                        URI sourceURI = URI.create(split[1]);
                        if (StringUtils.isNotBlank(hash256)
                                && (StringUtils.equals(resourceURI.toString(), sourceURI.toString())
                                || StringUtils.equals(hashCandidate, hash256))) {
                            URI localResourceURI = resourceURI;
                            if (!isJarResource(resourceURI)) {
                                localResourceURI = new File(accessFile.getParent(), hash256).toURI();// resource inside of cached archive
                            }
                            Date accessedAt = ISODateTimeFormat.dateTimeParser().withZoneUTC().parseDateTime(split[3]).toDate();
                            meta = new CachedURI(namespace, getRemoteJarURIIfNeeded(sourceURI, resourceURI), localResourceURI, hash256, accessedAt);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("unexpected exception on getting meta for [" + resourceURI + "]", e);
        }
        return meta;

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

