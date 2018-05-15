package org.eol.globi;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Version {

    public static String getVersion() {
        String version = Version.class.getPackage().getImplementationVersion();
        return StringUtils.isBlank(version) ? "dev" : version;
    }

    private static String getManifestAttributeValue(String attributeName) {
        InputStream resourceAsStream = Version.class.getResourceAsStream("/META-INF/MANIFEST.MF");
        return valueFromStream(resourceAsStream, attributeName);
    }

    static String valueFromStream(InputStream resourceAsStream, String attributeName) {
        String version = null;
        if (resourceAsStream != null) {
            try {
                Manifest manifest = new Manifest(resourceAsStream);
                Attributes attributes = manifest.getMainAttributes();
                version = attributes.getValue(attributeName);
            } catch (IOException e) {
                //
            }
        }
        return version;
    }

    public static String getVersionInfo(Class mainClass) {
        return mainClass.getSimpleName() + " [version: " + getVersion() + "]";
    }

    public static String getGitHubBaseUrl() {
        String sha = getManifestAttributeValue("Git-Commit-Sha");
        return "https://github.com/jhpoelen/eol-globi-data/blob/" + (sha == null ? "master" : sha);
    }
}
