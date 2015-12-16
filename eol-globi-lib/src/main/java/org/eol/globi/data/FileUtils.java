package org.eol.globi.data;

import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class FileUtils {
    public static BufferedReader getUncompressedBufferedReader(InputStream is, String characterEncoding) throws IOException {
        // http://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
        return new BufferedReader(new InputStreamReader(new BOMInputStream(is), characterEncoding));
    }

}
