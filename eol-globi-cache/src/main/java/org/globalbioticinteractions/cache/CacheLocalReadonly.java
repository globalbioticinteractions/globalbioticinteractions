package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;

public class CacheLocalReadonly implements Cache {
    private final static Logger LOG = LoggerFactory.getLogger(CacheLocalReadonly.class);

    private final String namespace;
    private final String cachePath;
    private ResourceService resourceServiceLocal;
    private final ContentPathFactory contentPathFactory;
    private final ProvenancePathFactory provenancePathFactory;

    public CacheLocalReadonly(String namespace,
                              String cachePath,
                              ResourceService resourceService,
                              ContentPathFactory contentPathFactory,
                              ProvenancePathFactory provenancePathFactory) {
        this.namespace = namespace;
        this.cachePath = cachePath;
        this.resourceServiceLocal = resourceService;
        this.contentPathFactory = contentPathFactory;
        this.provenancePathFactory = provenancePathFactory;
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
    public ContentProvenance provenanceOf(URI resourceURI) {
        return getContentProvenance(resourceURI, this.cachePath, this.namespace, this.contentPathFactory, provenancePathFactory);
    }

    public static ContentProvenance getContentProvenance(URI resourceURI,
                                                         String cachePath,
                                                         String namespace,
                                                         final ContentPathFactory contentPathFactory,
                                                         final ProvenancePathFactory provenancePathFactory) {
        AtomicReference<ContentProvenance> meta = new AtomicReference<>(null);
        File cacheDirForNamespace = CacheUtil.findCacheDirForNamespace(cachePath, namespace);
        ContentPath contentPath = contentPathFactory.getContentPath(cacheDirForNamespace);
        File accessFile = ProvenanceLog.getProvenanceLogFile(provenancePathFactory.getProvenancePath(cacheDirForNamespace));
        if (accessFile.exists()) {
            try {

                String hashCandidate = getHashCandidate(resourceURI, cacheDirForNamespace.toURI());
                LineReaderFactory lineReaderFactory = new ReverseLineReaderFactoryImpl();
                ProvenanceLog.parseProvenanceLogFile(accessFile, new ProvenanceLog.ProvenanceEntryListener() {
                    @Override
                    public void onValues(String[] values) {
                        if (values.length > 3) {
                            URI sourceURI = URI.create(values[1]);
                            String sha256 = values[2];
                            String accessedAt = StringUtils.trim(values[3]);
                            if (StringUtils.isNotBlank(sha256)) {
                                ContentProvenance provenance = getProvenance(resourceURI, hashCandidate, sourceURI, sha256, accessedAt, namespace, contentPath);
                                if (provenance != null) {
                                    meta.set(provenance);
                                }
                            }
                        }
                    }

                    @Override
                    public boolean shouldContinue() {
                        return meta.get() == null;
                    }
                }, lineReaderFactory);
            } catch (DatasetRegistryException e) {
                LOG.error("unexpected exception on getting meta for [" + resourceURI + "]", e);
            }
        }
        return meta.get();
    }

    public static ContentProvenance getProvenance(URI resourceURI,
                                                  String localArchiveSha256,
                                                  URI sourceURI,
                                                  String sha256,
                                                  String accessedAt,
                                                  String namespace,
                                                  ContentPath contentPath) {
        ContentProvenance meta = null;
        if (inCachedArchive(localArchiveSha256, sha256)) {
            meta = new ContentProvenance(namespace, getRemoteJarURIIfNeeded(sourceURI, resourceURI), resourceURI, sha256, accessedAt);
        } else if ((StringUtils.equals(resourceURI.toString(), sourceURI.toString())
                && !inCachedArchive(localArchiveSha256, sha256))) {
            meta = new ContentProvenance(namespace, sourceURI, contentPath.forContentId(sha256), sha256, accessedAt);
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
        ContentProvenance contentProvenance = provenanceOf(resourceURI);
        URI resourceLocalURI = contentProvenance == null ? null : contentProvenance.getLocalURI();
        return resourceLocalURI == null
                ? null
                : resourceServiceLocal.retrieve(resourceLocalURI);
    }

}

