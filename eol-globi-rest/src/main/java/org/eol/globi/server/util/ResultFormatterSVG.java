package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResultFormatterSVG implements ResultFormatter {

    @Override
    public String format(final String content) throws ResultFormattingException {
        JsonNode results;
        try {
            results = RequestHelper.parse(content);
        } catch (IOException e) {
            throw new ResultFormattingException("failed to parse", e);
        }
        JsonNode rows = results.get("data");
        String badge = rows != null && rows.size() > 0 ? "known.svg" : "unknown.svg";
        try {
            return IOUtils.toString(getClass().getResourceAsStream(badge), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResultFormattingException("failed to render badge", e);
        }
    }

}