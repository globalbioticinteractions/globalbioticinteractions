package org.eol.globi;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Version {

    public static String getVersion() {
        InputStream resourceAsStream = Version.class.getResourceAsStream("/META-INF/MANIFEST.MF");
        String version = versionFromStream(resourceAsStream);

        return version == null ? "dev" : version;
    }

    public static String versionFromStream(InputStream resourceAsStream) {
        String version = null;
        if (resourceAsStream != null) {
            try {
                Manifest manifest = new Manifest(resourceAsStream);
                Attributes attributes = manifest.getMainAttributes();
                version = attributes.getValue("Implementation-Version");
            } catch (IOException e) {
                //
            }
        }
        return version;
    }

    public static String getVersionInfo(Class mainClass) {
        return mainClass.getSimpleName() + " [version: " + getVersion() + "]";
    }
}
