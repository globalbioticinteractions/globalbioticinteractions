package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class ResourceUtil {

    public static final String DATA_DIR = "shapefiles.dir";

    public static URI fromDataDir(URI shapeFile) {
        URI resourceURI = null;
        String shapeFileDir = System.getProperty(DATA_DIR);
        if (StringUtils.isNotBlank(shapeFileDir)) {
            File file = new File(shapeFileDir + shapeFile);
            resourceURI = file.toURI();
        }
        return resourceURI;
    }

    public static InputStream asInputStream(String resource) throws IOException {
        return asInputStream(resource, inStream -> inStream);
    }

    public static InputStream asInputStream(String resource, InputStreamFactory factory) throws IOException {
        return asInputStream(URI.create(resource), factory);
    }

    public static InputStream asInputStream(URI resource, InputStreamFactory factory) throws IOException {
        try {
            InputStream is;

            ResourceService resourceService = new ResourceServiceFactoryRemote(factory)
                    .serviceForResource(resource);

            if (resourceService != null) {
                is = resourceService.retrieve(resource);
            } else {
                resourceService = new ResourceServiceFactoryLocal(factory)
                        .serviceForResource(resource);
                is = resourceService.retrieve(resource);
            }

            if (StringUtils.endsWith(resource.toString(), ".gz")) {
                is = new GZIPInputStream(is);
            }

            return is;
        } catch (IOException ex) {
            throw new IOException("issue accessing [" + resource + "]", ex);
        }
    }

    public static boolean isFileURI(URI resource) {
        return StringUtils.startsWith(resource.getScheme(), "file");
    }

    public static URI getAbsoluteResourceURI(URI context, URI resourceName) {
        return resourceName.isAbsolute()
                ? resourceName
                : absoluteURIFor(context, resourceName);
    }

    public static InputStream getCachedRemoteInputStream(URI resourceURI, InputStreamFactory factory) throws IOException {
        HttpGet request = new HttpGet(resourceURI);
        try {
            HttpResponse response = HttpUtil.getHttpClient().execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            try (InputStream content = response.getEntity().getContent()) {
                return cacheAndOpenStream(content, factory);
            }
        } finally {
            request.releaseConnection();
        }

    }

    public static InputStream cacheAndOpenStream(InputStream is, InputStreamFactory factory) throws IOException {
        File tempFile = File.createTempFile("globiRemote", "tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            IOUtils.copy(factory.create(is), fos);
            fos.flush();
        }
        return new FileInputStream(tempFile);
    }

    private static URI absoluteURIFor(URI context, URI resourceName) {
        String resourceNameNoSlashPrefix = StringUtils.startsWith(resourceName.toString(), "/")
                ? StringUtils.substring(resourceName.toString(), 1)
                : resourceName.toString();
        String contextString = context.toString();
        String contextNoSlashSuffix = StringUtils.endsWith(contextString, "/")
                ? StringUtils.substring(contextString, 0, contextString.length() - 1)
                : contextString;

        return URI.create(contextNoSlashSuffix + "/" + resourceNameNoSlashPrefix);
    }


    public static String contentToString(URI uri) throws IOException {
        String response;
        if ("file".equals(uri.getScheme()) || "jar".equals(uri.getScheme())) {
            response = IOUtils.toString(uri.toURL(), StandardCharsets.UTF_8);
        } else {
            response = HttpUtil.getContent(uri);
        }
        return response;
    }

    private static class ResourceServiceFactoryLocal implements ResourceServiceFactory {

        private final InputStreamFactory factory;

        public ResourceServiceFactoryLocal(InputStreamFactory factory) {
            this.factory = factory;
        }

        @Override
        public ResourceService serviceForResource(URI resource) {
            ResourceService resourceService = null;
            if (isFileURI(resource)) {
                resourceService = new ResourceServiceLocalFile(factory);
            } else if (StringUtils.startsWith(resource.toString(), "jar:file:/")) {
                resourceService = new ResourceServiceLocalJarResource(factory);
            } else {
                resourceService = new ResourceServiceClasspathResource(factory);
            }

            return resourceService;
        }
    }
}
