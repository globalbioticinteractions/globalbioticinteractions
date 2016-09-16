package org.eol.globi.server.util;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
            return IOUtils.toString(getClass().getResourceAsStream(badge), Charsets.UTF_8);
        } catch (IOException e) {
            throw new ResultFormattingException("failed to render badge", e);
        }
    }
}