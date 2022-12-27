package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceDataDir implements ResourceService {

    private final String dataDir;

    public ResourceServiceDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return new FileInputStream(
                new File(
                        fromDataDir(resourceName)
                )
        );
    }

    private URI fromDataDir(URI resourceName) throws IOException {

        File dataFile = resourceName.isAbsolute()
                ? new File(resourceName)
                : new File(resourceName.getPath());
        if (!dataFile.exists()) {
            File dataRoot = new File(dataDir);
            if (!dataRoot.exists()) {
                throw new IOException("provided data dir [" + dataDir + "] does not exist.");
            }
            dataFile = new File(dataRoot, resourceName.getPath());
        }

        return dataFile.toURI();
    }

}
