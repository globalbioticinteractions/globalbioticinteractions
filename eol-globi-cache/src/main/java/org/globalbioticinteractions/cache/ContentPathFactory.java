package org.globalbioticinteractions.cache;

import java.io.File;

public interface ContentPathFactory {
    ContentPath getContentPath(File baseDir);
}
