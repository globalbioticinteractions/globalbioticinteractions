package org.eol.globi.server.util;

import java.util.HashMap;
import java.util.Map;

public class ResultTestFormatterFactory {

    private final static Map<String, ResultFormatter> TYPE_TO_FORMATTER_MAP = new HashMap<String, ResultFormatter>() {{
        put("json", new ResultFormatterJSON());
        put("csv", new ResultFormatterCSV());
        put("tsv", new ResultFormatterTSV());
        put("json.v2", new ResultFormatterJSONv2());
        put("dot", new ResultFormatterDOT());
    }};

    public ResultFormatter create(String type) {
        return TYPE_TO_FORMATTER_MAP.get(type == null ? "json" : type);
    }
}
