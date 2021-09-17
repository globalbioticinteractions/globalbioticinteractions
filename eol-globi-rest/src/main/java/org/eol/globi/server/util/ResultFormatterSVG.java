package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResultFormatterSVG implements ResultFormatter {

    @Override
    public String format(final String content) throws ResultFormattingException {
        JsonNode rowsAndMeta;
        try {
            rowsAndMeta = RequestHelper.getRowsAndMetas(content);

        } catch (IOException e) {
            throw new ResultFormattingException("failed to parse", e);
        }

        String badge = rowsAndMeta != null && rowsAndMeta.size() > 0 ? "known.svg" : "unknown.svg";
        try {
            return IOUtils.toString(getClass().getResourceAsStream(badge), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResultFormattingException("failed to render badge", e);
        }
    }

}