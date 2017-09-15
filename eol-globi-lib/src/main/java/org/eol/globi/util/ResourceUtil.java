package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceUtil {

    public static final String SHAPEFILES_DIR = "shapefiles.dir";

    private static final BlobStore blobStore = new BlobStoreTmpCache();

    public static InputStream asInputStream(URI resource, Class clazz) throws IOException {
        return blobStore.asInputStream(resource.toString(), clazz);
    }

    public static InputStream asInputStream(final String resource, Class clazz) throws IOException {
        return blobStore.asInputStream(resource, clazz);
    }

    public static boolean resourceExists(URI descriptor) {
        return blobStore.resourceExists(descriptor);
    }

    public static URI getAbsoluteResourceURI(URI context, String resourceName) {
        return blobStore.getAbsoluteResourceURI(context, resourceName);
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
