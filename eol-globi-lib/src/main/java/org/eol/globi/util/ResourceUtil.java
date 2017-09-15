package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.Dataset;

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

    public static InputStream getResource(String resourceName, Dataset dataset) throws IOException {
        String mappedResource = mapResourceNameIfRequested(resourceName, dataset.getConfig());
        return ResourceUtil.asInputStream(dataset.getResourceURI(mappedResource), null);
    }

    public static URI getResourceURI(String resourceName, Dataset dataset) {
        URI archiveURI = dataset.getArchiveURI();
        return getResourceURI(resourceName, dataset, archiveURI);
    }

    public static URI getResourceURI(String resourceName, Dataset dataset, URI archiveURI) {
        String mappedResourceName = ResourceUtil.mapResourceNameIfRequested(resourceName, dataset.getConfig());
        return ResourceUtil.getAbsoluteResourceURI(archiveURI, mappedResourceName);
    }

    private static String mapResourceNameIfRequested(String resourceName, JsonNode config) {
        String mappedResource = resourceName;
        if (config != null && config.has("resources")) {
            JsonNode resources = config.get("resources");
            if (resources.isObject() && resources.has(resourceName)) {
                JsonNode resourceName1 = resources.get(resourceName);
                if (resourceName1.isTextual()) {
                    String resourceNameCandidate = resourceName1.asText();
                    mappedResource = StringUtils.isBlank(resourceNameCandidate) ? mappedResource : resourceNameCandidate;
                }
            }
        }
        return mappedResource;
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
