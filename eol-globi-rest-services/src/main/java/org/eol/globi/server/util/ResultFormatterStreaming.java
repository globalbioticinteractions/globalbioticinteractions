package org.eol.globi.server.util;

import java.io.InputStream;
import java.io.OutputStream;

public interface ResultFormatterStreaming extends ResultFormatter {

    void format(InputStream is, OutputStream os) throws ResultFormattingException;
}
