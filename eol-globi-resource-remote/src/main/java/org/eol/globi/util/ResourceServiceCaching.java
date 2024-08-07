package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
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

    protected static InputStream cacheAndOpenStream(InputStream is, InputStreamFactory factory) throws IOException {
        File tempFile = File.createTempFile("globiRemote", "tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            IOUtils.copy(factory.create(is), fos);
            fos.flush();
        }
        return new FileInputStream(tempFile);
    }

    public File getCacheDir() {
        return cacheDir;
    }
}
