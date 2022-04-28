package org.eol.globi.service;

import org.eol.globi.util.ResourceServiceLocal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public class CacheServiceUtil {

    public static BufferedReader createBufferedReader(String taxonResourceUrl, ResourceService resourceService) throws IOException {
        InputStream is = resourceService.retrieve(URI.create(taxonResourceUrl));
        if (is == null) {
            throw new IOException("failed to access [" + taxonResourceUrl + "]");
        }
        return new BufferedReader(new InputStreamReader(is));
    }

    public static void createCacheDir(File cacheDir) throws IOException {
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new IOException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }
    }

}
