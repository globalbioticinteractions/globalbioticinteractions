package org.eol.globi.server;

public interface ResultFormatter {

    public String format(String s) throws ResultFormattingException;
}
