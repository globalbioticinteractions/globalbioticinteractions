package org.eol.globi.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class FileUtils {
    public static BufferedReader getBufferedReaderUTF_8(InputStream gzipInputStream) throws IOException {
        GZIPInputStream is = new GZIPInputStream(gzipInputStream);
        return getUncompressedBufferedReaderUTF_8(is);
    }

    public static BufferedReader getUncompressedBufferedReaderUTF_8(InputStream is) throws IOException {
        return new BufferedReader(new InputStreamReader(is, "UTF-8"));
    }
}
