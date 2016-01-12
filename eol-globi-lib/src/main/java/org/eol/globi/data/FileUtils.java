package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
    public static BufferedReader getUncompressedBufferedReader(InputStream is, String characterEncoding) throws IOException {
        // http://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
        return new BufferedReader(new InputStreamReader(new BOMInputStream(is), characterEncoding));
    }

    public static File saveToTmpFile(ZipInputStream zipInputStream, ZipEntry entry) throws IOException {
        File tempFile = File.createTempFile(entry.getName(), "tmp");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        IOUtils.copy(zipInputStream, fos);
        fos.flush();
        IOUtils.closeQuietly(fos);
        return tempFile;
    }
}
