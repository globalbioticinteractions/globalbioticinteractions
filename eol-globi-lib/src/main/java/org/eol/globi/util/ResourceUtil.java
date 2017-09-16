package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceUtil {

    public static final String SHAPEFILES_DIR = "shapefiles.dir";

    private static final ResourceCache RESOURCE_CACHE = new ResourceCacheTmp();

    public static InputStream asInputStream(final String resource) throws IOException {
        return RESOURCE_CACHE.asInputStream(resource);
    }

    public static URI fromShapefileDir(String shapeFile) {
        URI resourceURI = null;
        String shapeFileDir = System.getProperty(SHAPEFILES_DIR);
        if (StringUtils.isNotBlank(shapeFileDir)) {
            File file = new File(shapeFileDir + shapeFile);
            resourceURI = file.toURI();
        }
        return resourceURI;
    }


}
