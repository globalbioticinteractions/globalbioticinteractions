package org.eol.globi.server.util;

import java.io.IOException;

public class ResultFormattingException extends IOException {
    public ResultFormattingException(String msg, Throwable e) {
        super(msg, e);
    }
}
