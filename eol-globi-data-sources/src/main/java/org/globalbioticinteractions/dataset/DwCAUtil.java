package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.gbif.dwc.Archive;
import org.gbif.dwc.DwcFiles;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DwCAUtil {

    public static Archive archiveFor(URI archiveURI, String tmpDir) throws IOException {
        Archive archive;
        Path myArchiveFile = Paths.get(archiveURI);
        if (myArchiveFile.toFile().isFile()) {
            if (StringUtils.isBlank(tmpDir)) {
                throw new IllegalArgumentException("cannot read [" + archiveURI + "] without a tmpDir");
            }
            Path extractToFolder = Paths.get(tmpDir);
            archive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder);
        } else {
            archive = DwcFiles.fromLocation(myArchiveFile);
        }
        return archive;
    }
}
