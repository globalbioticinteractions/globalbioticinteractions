package org.eol.globi.util;

import com.Ostermiller.util.ExcelCSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Automatically related streams when no more lines are available
 */
public class AutoCloseCSVExcelParser extends ExcelCSVParser {


    public AutoCloseCSVExcelParser(Reader in) {
        super(in);
    }

    public AutoCloseCSVExcelParser(InputStream in) {
        super(in);
    }

    @Override
    public String[] getLine() throws IOException {
        String[] line = super.getLine();
        if (line == null) {
            super.close();
        }
        return line;
    }
}
