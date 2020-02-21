package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class CacheLocalReadonly implements Cache {
    private final static Log LOG = LogFactory.getLog(CacheLocalReadonly.class);

    private final String namespace;
    private final String cachePath;
    private final InputStreamFactory inputStreamFactory;

    public CacheLocalReadonly(String namespace, String cachePath) {
        this(namespace, cachePath, inStream -> inStream);
    }

    public CacheLocalReadonly(String namespace, String cachePath, InputStreamFactory factory) {
        this.namespace = namespace;
        this.cachePath = cachePath;
        this.inputStreamFactory = factory;
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
    public URI getLocalURI(URI resourceName) {
        ContentProvenance contentProvenance = provenanceOf(resourceName);
        return contentProvenance == null ? null : contentProvenance.getLocalURI();
    }

    @Override
    public ContentProvenance provenanceOf(URI resourceURI) {
        return getContentProvenance(resourceURI, this.cachePath, this.namespace);
    }

    public static ContentProvenance getContentProvenance(URI resourceURI, String cachePath, String namespace) {
        ContentProvenance meta = null;
        File accessFile;
        try {
            File cacheDirForNamespace = CacheUtil.findCacheDirForNamespace(cachePath, namespace);

            String hashCandidate = getHashCandidate(resourceURI, cacheDirForNamespace.toURI());
            accessFile = ProvenanceLog.findProvenanceLogFile(namespace, cachePath);
            if (accessFile.exists()) {
                String[] rows = IOUtils.toString(accessFile.toURI(), StandardCharsets.UTF_8).split("\n");
                for (String row : rows) {
                    String[] split = CSVTSVUtil.splitTSV(row);
                    if (split.length > 3) {
                        URI sourceURI = URI.create(split[1]);
                        String sha256 = split[2];
                        String accessedAt = StringUtils.trim(split[3]);
                        if (StringUtils.isNotBlank(sha256)) {
                            ContentProvenance provenance = null;
                            if (resourceURI.toString().startsWith("hash://sha256/")) {
                                if (StringUtils.equals("hash://sha256/" + sha256, resourceURI.toString())) {
                                    URI localResourceURI = new File(cacheDirForNamespace, sha256).toURI();
                                    provenance = new ContentProvenance(namespace, sourceURI, localResourceURI, sha256, accessedAt);
                                }
                            } else {
                                provenance = getProvenance(resourceURI, hashCandidate, sourceURI, sha256, accessedAt, cacheDirForNamespace, namespace);
                            }
                            meta = provenance == null ? meta : provenance;
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("unexpected exception on getting meta for [" + resourceURI + "]", e);
        }
        return meta;
    }

    public static ContentProvenance getProvenance(URI resourceURI, String localArchiveSha256, URI sourceURI, String sha256, String accessedAt, File cacheDir, String namespace) {
        ContentProvenance meta = null;
        if (inCachedArchive(localArchiveSha256, sha256)) {
            meta = new ContentProvenance(namespace, getRemoteJarURIIfNeeded(sourceURI, resourceURI), resourceURI, sha256, accessedAt);
        } else if ((StringUtils.equals(resourceURI.toString(), sourceURI.toString())
                && !inCachedArchive(localArchiveSha256, sha256))) {
            URI localResourceURI = new File(cacheDir, sha256).toURI();
            meta = new ContentProvenance(namespace, sourceURI, localResourceURI, sha256, accessedAt);
        }
        return meta;
    }

    public static boolean inCachedArchive(String localArchiveSha256, String sha256) {
        return StringUtils.isNotBlank(localArchiveSha256) && StringUtils.equals(localArchiveSha256, sha256);
    }

    static String getHashCandidate(URI resourceURI, URI cacheDir) {
        String hashCandidate = null;
        URI candidateURI = resourceURI;
        candidateURI = getDatasetArchiveURI(candidateURI);

        if (candidateURI != null && StringUtils.startsWith(candidateURI.toString(), cacheDir.toString())) {
            hashCandidate = StringUtils.replace(candidateURI.toString(), cacheDir.toString(), "");
        }
        if (candidateURI != null && StringUtils.startsWith(candidateURI.toString(), "hash://sha256/")) {
            hashCandidate = StringUtils.replace(candidateURI.toString(), "hash://sha256/", "");
        }
        return hashCandidate;
    }

    private static URI getDatasetArchiveURI(URI candidateURI) {
        if (isJarResource(candidateURI)) {
            URLConnection urlConnection = null;
            try {
                urlConnection = candidateURI.toURL().openConnection();
                if (urlConnection instanceof JarURLConnection) {
                    candidateURI = ((JarURLConnection) urlConnection).getJarFileURL().toURI();
                }

            } catch (IOException | URISyntaxException e) {
                // ignore
            } finally {
                if (urlConnection != null) {
                    try {
                        IOUtils.closeQuietly(urlConnection.getInputStream());
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
        return candidateURI;
    }

    static boolean isJarResource(URI candidateURI) {
        return candidateURI != null && "jar".equals(candidateURI.getScheme());
    }

    @Override
    public InputStream retrieve(URI resourceURI) throws IOException {
        URI resourceURI1 = getLocalURI(resourceURI);
        return resourceURI1 == null ? null : ResourceUtil.asInputStream(resourceURI1.toString(), getInputStreamFactory());
    }

    private InputStreamFactory getInputStreamFactory() {
        return this.inputStreamFactory;
    }
}

