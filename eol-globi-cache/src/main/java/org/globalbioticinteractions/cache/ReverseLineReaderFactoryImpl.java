package org.globalbioticinteractions.cache;

import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.File;
import java.io.IOException;
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
