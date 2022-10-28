package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ReverseLineReaderFactoryImpl implements LineReaderFactory {
    public LineReader createLineReader(File file) throws IOException {
        return new LineReader() {

            final ReversedLinesFileReader reader = new ReversedLinesFileReader(file, StandardCharsets.UTF_8);

            @Override
            public void close() throws IOException {
                reader.close();
            }

            @Override
            public String readLine() throws IOException {
                return reader.readLine();
            }
        };
    }

    ;
}
