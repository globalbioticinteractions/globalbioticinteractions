package org.globalbioticinteractions.pensoft;

import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;

public class TableUtil {
    public static Document parseHtml(String htmlString) {
        final Document doc = Jsoup.parse(htmlString);
        configureDocument(doc);
        return doc;
    }

    public static void configureDocument(Document doc) {
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .charset(StandardCharsets.UTF_8)
                .indentAmount(0)
                .escapeMode(Entities.EscapeMode.xhtml)
                .outline(false)
                .prettyPrint(false);
    }

    public static boolean hasFullRowColumnSpan(Document doc, Integer nativeRowLength) {
        boolean hasFullRowColumnSpan = false;
        Elements rows = doc.select("tr");
        for (Element row : rows) {
            Elements rowColumns = row.select("td");
            for (Element rowColumn : rowColumns) {
                final String attr = rowColumn.attr("colspan");
                final int colSpan = NumberUtils.toInt(attr, 1);
                if (colSpan == nativeRowLength) {
                    hasFullRowColumnSpan = true;
                }
            }
        }
        return hasFullRowColumnSpan;
    }
}
