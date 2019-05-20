package org.eol.globi.util;

import com.Ostermiller.util.CSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Automatically related streams when no more lines are available
 */
public class AutoCloseCSVParser extends CSVParser {


    public AutoCloseCSVParser(Reader in) {
        super(in);
    }

    public AutoCloseCSVParser(InputStream in) {
        super(in);
    }

    public AutoCloseCSVParser(InputStream in, char delimiter) {
        super(in, delimiter);
    }

    @Override
    public String[] getLine() throws IOException {
        String[] line = super.getLine();
        if (line == null) {
            super.close();
        }
        return line;
    }

    @Override
    public String[][] getAllValues() throws IOException {
        String[][] allValues = super.getAllValues();
        super.close();
        return allValues;
    }

}
