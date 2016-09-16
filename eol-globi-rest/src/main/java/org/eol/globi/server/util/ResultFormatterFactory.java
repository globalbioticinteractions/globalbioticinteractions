package org.eol.globi.server.util;

import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

public class ResultFormatterFactory {

    private final static Map<MediaType, ResultFormatter> TYPE_TO_FORMATTER_MAP = new HashMap<MediaType, ResultFormatter>() {{
        put(MediaType.parseMediaType("application/json;charset=UTF-8"), new ResultFormatterJSON());
        put(MediaType.parseMediaType("application/ld+json;charset=UTF-8"), new ResultFormatterJSONLD());
        put(MediaType.parseMediaType("text/csv;charset=UTF-8"), new ResultFormatterCSV());
        put(MediaType.parseMediaType("text/tab-separated-values;charset=UTF-8"), new ResultFormatterTSV());
        // a trick to distinguish JSONv2 from JSON
        put(MediaType.parseMediaType("text/html;charset=UTF-8"), new ResultFormatterJSONv2());
        put(MediaType.parseMediaType("text/vnd.graphviz;charset=UTF-8"), new ResultFormatterDOT());
        put(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"), new ResultFormatterSVG());
    }};
    public static final MediaType JSON = MediaType.parseMediaType("application/json;charset=UTF-8");

    public ResultFormatter create(MediaType type) {
        return TYPE_TO_FORMATTER_MAP.get(type == null ? JSON : type);
    }
}
