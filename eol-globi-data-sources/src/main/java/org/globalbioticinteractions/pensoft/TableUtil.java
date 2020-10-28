package org.globalbioticinteractions.pensoft;

import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

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

    public static boolean isRectangularTable(Document doc) {
        return collectDistinctRowLengths(doc).size() == 1;
    }

    public static Set<Integer> collectDistinctRowLengths(Document doc) {
        Elements rows = doc.select("tr");

        Set<Integer> distinctRowLengths = new HashSet<>();
        for (Element row : rows) {
            countRowLengthForType(distinctRowLengths, row, "td");
            countRowLengthForType(distinctRowLengths, row, "th");
        }
        return distinctRowLengths;
    }

    public static void countRowLengthForType(Set<Integer> rowLengths, Element row, String cellType) {
        Elements rowValues = row.select(cellType);
        int rowLength = 0;
        for (Element rowValue : rowValues) {
            final String attr = rowValue.attr("colspan");
            final int colSpan = NumberUtils.toInt(attr, 1);
            if (colSpan > 1) {
                rowLength += colSpan;
            } else {
                rowLength++;
            }
        }
        if (rowLength > 0) {
            rowLengths.add(rowLength);
        }
    }

    public static Elements selectReferences(Element element) {
        return element.select(new Evaluator.TagEndsWith("xref"));
    }

    public static Elements selectTaxonNames(Element element) {
        return element.select(new Evaluator.TagEndsWith("tp:taxon-name"));
    }
}
