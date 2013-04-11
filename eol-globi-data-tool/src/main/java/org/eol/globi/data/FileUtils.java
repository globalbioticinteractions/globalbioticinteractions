package org.eol.globi.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class FileUtils {
    public static BufferedReader getBufferedReader(InputStream gzipInputStream, String characterEncoding) throws IOException {
        GZIPInputStream is = new GZIPInputStream(gzipInputStream);
        return getUncompressedBufferedReader(is, characterEncoding);
    }

    public static BufferedReader getUncompressedBufferedReader(InputStream is, String characterEncoding) throws IOException {
        return new BufferedReader(new InputStreamReader(is, characterEncoding));
    }
}
