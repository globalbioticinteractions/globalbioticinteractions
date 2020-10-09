package org.globalbioticinteractions.pensoft;

import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExpandColumnSpans implements TableProcessor {

    @Override
    public String process(String input) {
        Document doc = TableUtil.parseHtml(input);

        Set<Integer> distinctRowLengths = distinctRowLengths(doc);

        if (distinctRowLengths.size() == 1) {
            Integer nativeRowLength = distinctRowLengths.iterator().next();
            boolean hasFullRowColumnSpan = TableUtil.hasFullRowColumnSpan(doc, nativeRowLength);

            if (hasFullRowColumnSpan) {
                List<Element> rowsToBeDeleted = new ArrayList<>();
                Elements rows = doc.select("tr");
                Element lastSpannedValue = null;
                for (Element row : rows) {
                    Elements headerValues = row.select("th");
                    if (headerValues.size() > 0) {
                        Element first = headerValues.first();
                        first.before("<th></th>");
                    }

                    Elements rowValues = row.select("td");
                    if (rowValues.iterator().hasNext()) {
                        Element value = rowValues.iterator().next();
                        final String attr = value.attr("colspan");
                        final int colSpan = NumberUtils.toInt(attr, 1);
                        if (colSpan == nativeRowLength) {
                            lastSpannedValue = value.clone();
                            lastSpannedValue.attr("colspan", "1");
                            rowsToBeDeleted.add(row);
                        } else {
                            if (lastSpannedValue == null) {
                                value.before("<td></td>");
                            } else {
                                value.before(lastSpannedValue.clone());
                            }
                        }
                    }
                }
                rowsToBeDeleted.forEach(Node::remove);
            }
        }
        return doc.select("table").toString();
    }

    public Set<Integer> distinctRowLengths(Document doc) {
        Elements rows = doc.select("tr");

        Set<Integer> distinctRowLengths = new HashSet<>();
        for (Element row : rows) {
            countRowLengthForType(distinctRowLengths, row, "td");
            countRowLengthForType(distinctRowLengths, row, "th");
        }
        return distinctRowLengths;
    }

    public void countRowLengthForType(Set<Integer> rowLengths, Element row, String cellType) {
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
}
