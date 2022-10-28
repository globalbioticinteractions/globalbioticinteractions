package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;

public class LineReaderFactoryImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void readFirstLine() throws IOException {

        File file = getTestFile();

        LineReader lineReader = new LineReaderFactoryImpl()
                .createLineReader(file);

        String firstLine = lineReader.readLine();

        assertThat(firstLine, Is.is("one"));
    }

    private File getTestFile() throws IOException {
        File file = folder.newFile();
        FileUtils.writeStringToFile(file, "one\ntwo\nthree", StandardCharsets.UTF_8);
        return file;
    }

    @Test
    public void readLastLine() throws IOException {
        File file = getTestFile();

        LineReader lineReader = new LineReaderFactoryImpl()
                .createLineReader(file);

        String lastLine = null;
        String line;
        while ((line = lineReader.readLine()) != null) {
            lastLine = line;
        }

        assertThat(lastLine, Is.is("three"));
    }

}