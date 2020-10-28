package org.globalbioticinteractions.pensoft;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static org.eol.globi.data.DatasetImporterForPensoft.expandSpannedRows;

public class ExpandRowSpans implements TableProcessor {

    @Override
    public String process(String input) {
        Document doc = TableUtil.parseHtml(input);

        Elements rows = doc.select("tr");
        for (Element row : rows) {
            Elements rowColumns = row.select("td");
            expandSpannedRows(row, rowColumns);
        }
        return doc.select("table").toString();
    }
}
