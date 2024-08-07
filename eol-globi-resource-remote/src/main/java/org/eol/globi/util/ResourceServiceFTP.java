package org.eol.globi.util;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceFTP extends ResourceServiceCaching {
    private final InputStreamFactory factory;

    public ResourceServiceFTP(InputStreamFactory factory, File cacheDir) {
        super(cacheDir);
        this.factory = factory;
    }

    @Override
    public InputStream retrieve(URI resource) throws IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(resource.getHost());
            ftpClient.enterLocalPassiveMode();
            ftpClient.login("anonymous", "info@globalbioticinteractions.org");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE, FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);

            return ftpClient.isConnected()
                    ? cacheAndOpenStream(ftpClient.retrieveFileStream(resource.getPath()), factory, getCacheDir())
                    : null;
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
        }
    }
}
