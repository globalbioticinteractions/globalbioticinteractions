package org.globalbioticinteractions.pensoft;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class TablePreprocessor implements TableProcessor {

    @Override
    public String process(String input) {
        String htmlTrimmed = StringUtils
                .replacePattern(input, "\\\n\\s*", "");
        String htmlTrimmed2 = StringUtils
                .replacePattern(htmlTrimmed, "(<bold>)|(</bold>)|(<italic>)|(</italic>)|(<br/>)|(\\s*&lt;br/&gt;\\s*)", "");
        final Document doc = TableUtil.parseHtml(htmlTrimmed2);

        Elements table = doc.select("table");
        return table.toString();
    }

}
