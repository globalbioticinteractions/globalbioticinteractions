package org.eol.globi.service;

import org.eol.globi.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class CacheServiceUtil {

    public static BufferedReader createBufferedReader(String taxonResourceUrl) throws IOException {
        return new BufferedReader(new InputStreamReader(ResourceUtil.asInputStream(taxonResourceUrl)));
    }

    public static void createCacheDir(File cacheDir) throws PropertyEnricherException {
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }
    }

}
