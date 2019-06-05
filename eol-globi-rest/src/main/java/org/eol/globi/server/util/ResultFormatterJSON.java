package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ResultFormatterJSON implements ResultFormatterStreaming {

    @Override
    public String format(String s) throws ResultFormattingException {
        return s;
    }

    @Override
    public void format(InputStream is, OutputStream os) throws ResultFormattingException {
        try (InputStream inputStream = is) {
            IOUtils.copy(inputStream, os);
            os.flush();
        } catch (IOException e) {
            throw new ResultFormattingException("failed to format incoming stream", e);
        }
    }


}
