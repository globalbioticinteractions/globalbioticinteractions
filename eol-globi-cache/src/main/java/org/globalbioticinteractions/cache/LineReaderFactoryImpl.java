package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LineReaderFactoryImpl implements LineReaderFactory {
    public LineReader createLineReader(File file) throws IOException {
        return new LineReader() {

            final InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            final BufferedReader bufferedReader = IOUtils.toBufferedReader(reader);

            @Override
            public void close() throws IOException {
                reader.close();
            }

            @Override
            public String readLine() throws IOException {
                return bufferedReader.readLine();
            }
        };
    }

    ;
}
