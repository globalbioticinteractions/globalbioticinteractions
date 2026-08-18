package org.eol.globi.data;

import java.nio.charset.StandardCharsets;

public class CharsetConstant {
    public static final String UTF8 = StandardCharsets.UTF_8.name();
    public static final String SEPARATOR_CHAR = "|";
    public static final String SEPARATOR = " " + SEPARATOR_CHAR + " ";
    public static final String SEPARATOR_START_LIST = SEPARATOR_CHAR + " ";
    public static final String SEPARATOR_END_LIST = " " + SEPARATOR_CHAR;
    public static final String LANG_SEPARATOR_CHAR = "@";
}
