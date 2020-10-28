package org.globalbioticinteractions.pensoft;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import static org.eol.globi.data.DatasetImporterForPensoft.expandSpannedRows;

public class AddColumnFromCaption implements TableProcessor {

    private final String caption;

    public AddColumnFromCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public String process(String input) {
        Document caption = TableUtil.parseHtml(this.caption);

        Elements nameElements = TableUtil.selectTaxonNames(caption);

        Document doc = TableUtil.parseHtml(input);

        if (nameElements.size() > 0) {
            List<Element> toBeRemoved = new ArrayList<>();
            Elements rows = doc.select("tr");
            for (Element row : rows) {
                Elements columnHeaders = row.select("th");
                if (columnHeaders.size() > 0) {
                    columnHeaders.first().before("<th>taxonNameInTableCaption</th>");
                } else {
                    toBeRemoved.add(row);
                    for (Element nameElement : nameElements) {
                        Element clonedRow = row.clone();
                        Elements rowValues = clonedRow.select("td");
                        if (rowValues.size() > 0) {
                            Element clonedValue = rowValues.first().clone();
                            clonedValue.children().forEach(Node::remove);
                            clonedValue.appendChild(nameElement.clone());
                            clonedRow.prependChild(clonedValue);
                        }
                        row.before(clonedRow);
                    }
                }

            }
            toBeRemoved.forEach(Node::remove);
        }
        return doc.select("table").toString();
    }
}
