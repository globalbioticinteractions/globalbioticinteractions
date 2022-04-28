package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class ResourceUtil {

    public static InputStream asInputStream(String resource) throws IOException {
        URI resource1 = URI.create(resource);
        try {
            return new ResourceServiceLocalAndRemote(inStream -> inStream).retrieve(resource1);
        } catch (IOException ex) {
            throw new IOException("issue accessing [" + resource1 + "]", ex);
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

}
