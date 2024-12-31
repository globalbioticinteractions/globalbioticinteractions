package org.eol.globi.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ProxyInputStream;
import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class ResourceServiceCaching implements ResourceService {

    private final File cacheDir;

    public ResourceServiceCaching(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    protected static InputStream cacheAndOpenStream(InputStream is, InputStreamFactory factory, File tmpDir) throws IOException {
        if (!tmpDir.exists()) {
            FileUtils.forceMkdir(tmpDir);
        }
        final File tmpFile = File.createTempFile("globiRemote", "tmp", tmpDir);
        tmpFile.deleteOnExit();
        return cacheAndOpenStream2(is, factory, tmpFile);
    }

    protected static InputStream cacheAndOpenStream2(InputStream is, InputStreamFactory factory, File tmpFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
            IOUtils.copy(factory.create(is), fos);
            fos.flush();
        }
        return new ProxyInputStream(new FileInputStream(tmpFile)) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    if (tmpFile.exists()) {
                        FileUtils.forceDelete(tmpFile);
                    }
                }
            }
        };
    }

    public File getCacheDir() {
        return cacheDir;
    }
}
