package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;

public class CacheLocalReadonly implements Cache {
    private final Log LOG = LogFactory.getLog(CacheLocalReadonly.class);

    private final String namespace;
    private final String cachePath;

    public CacheLocalReadonly(String namespace, String cachePath) {
        this.namespace = namespace;
        this.cachePath = cachePath;
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

            String hashCandidate = getHashCandidate(resourceURI, cacheDirForNamespace);
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
                            File cachedArchiveFile = new File(accessFile.getParent(), hash256);
                            Date accessedAt = ISODateTimeFormat.dateTimeParser().withZoneUTC().parseDateTime(split[3]).toDate();
                            meta = new CachedURI(namespace, sourceURI, cachedArchiveFile.toURI(), hash256, accessedAt);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("unexpected exception on getting meta for [" + resourceURI + "]", e);
        }
        return meta;

    }

    private String getHashCandidate(URI resourceURI, File cacheDirForNamespace) {
        String hashCandidate = null;
        if (StringUtils.startsWith(resourceURI.toString(), cacheDirForNamespace.toURI().toString())) {
            hashCandidate = StringUtils.replace(resourceURI.toString(), cacheDirForNamespace.toURI().toString(), "");
        }
        return hashCandidate;
    }

    @Override
    public InputStream asInputStream(URI resourceURI) throws IOException {
        URI resourceURI1 = asURI(resourceURI);
        return resourceURI1 == null ? null : resourceURI1.toURL().openStream();
    }
}

