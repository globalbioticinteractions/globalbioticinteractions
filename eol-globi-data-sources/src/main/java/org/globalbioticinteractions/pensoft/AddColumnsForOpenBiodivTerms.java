package org.globalbioticinteractions.pensoft;

import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AddColumnsForOpenBiodivTerms implements TableProcessor {

    interface TermSelector {
        List<Term> select(Element columnIndex);

        String typeName();

        Term asTerm(Element element);
    }

    @Override
    public String process(String input) {
        Document docOriginal = TableUtil.parseHtml(input);

        Document processedDoc = Stream.of(new NameTermSelector(), new ReferenceTermSelector())
                .reduce(docOriginal,
                        (d, term) -> processWithSelector(term, d),
                        (d1, d2) -> d1);

        return processedDoc.select("table").toString();
    }

    private Document processWithSelector(TermSelector selector, Document doc) {

        Set<Integer> distinctColumnsToBeAdded = collectColumnsToBeAdded(selector, doc);

        addHeadersForAddedColumns(doc, distinctColumnsToBeAdded, selector.typeName());

        Elements rows = doc.select("tr");
        for (Element row : rows) {
            Map<Integer, List<Term>> tableDataToBeAdded = new TreeMap<>();
            Elements rowColumns = row.select("td");
            if (rowColumns.size() != 0) {
                for (int i = 0; i < rowColumns.size(); i++) {
                    Element rowColumn = rowColumns.get(i);
                    final List<Term> names = selector.select(rowColumn);

                    if (names.size() > 0) {
                        tableDataToBeAdded.put(i, names);
                    }

                }

                Stack<Element> intermediateRows = new Stack<>();
                intermediateRows.push(row);
                for (Integer columnIndex : distinctColumnsToBeAdded) {
                    List<Term> expandedValues = tableDataToBeAdded.get(columnIndex);
                    if (expandedValues == null || expandedValues.size() == 0) {
                        expandedValues = Collections.emptyList();
                    }
                    List<Element> generatedRows = new ArrayList<>();

                    while (!intermediateRows.empty()) {
                        Element intermediateRow = intermediateRows.pop();
                        generatedRows.addAll(appendColumn(intermediateRow, columnIndex, expandedValues));
                        intermediateRow.remove();
                    }

                    intermediateRows.addAll(generatedRows);
                }
            }

        }
        return doc;
    }

    private void addHeadersForAddedColumns(Document doc, Set<Integer> distinctColumnsToBeAdded, String headerSuffix) {
        Elements rows3 = doc.select("tr");
        for (Element row : rows3) {
            Elements tableDataHeaders = row.select("th");
            if (tableDataHeaders.size() > 0) {
                for (Integer columnIndex : distinctColumnsToBeAdded) {
                    Element header = tableDataHeaders.get(columnIndex);

                    Element headerIdClone = header.clone();
                    headerIdClone.append("_expanded_" + headerSuffix + "_id");
                    header.siblingElements().last().after(headerIdClone);

                    Element headerLabelClone = header.clone();
                    headerLabelClone.append("_expanded_" + headerSuffix + "_name");
                    header.siblingElements().last().after(headerLabelClone);
                }
            }
        }
    }

    private Set<Integer> collectColumnsToBeAdded(TermSelector selector, Document doc) {
        Elements rows2 = doc.select("tr");
        Set<Integer> distinctColumnsToBeAdded = new TreeSet<>();
        for (Element row : rows2) {
            Elements rowColumns = row.select("td");

            for (int i = 0; i < rowColumns.size(); i++) {
                Element rowColumn = rowColumns.get(i);
                final List<Term> names = selector.select(rowColumn);

                if (names.size() > 0) {
                    distinctColumnsToBeAdded.add(i);
                }

            }
        }
        return distinctColumnsToBeAdded;
    }

    private List<Element> appendColumn(Element row, int tableDataColumnIndex, List<Term> expandedValues) {
        List<Element> generatedRows = new ArrayList<>();


        for (Term name : expandedValues) {
            ArrayList<Pair<Integer, Term>> replacementValues = new ArrayList<>();
            replacementValues.add(Pair.of(tableDataColumnIndex, name));
            Element clonedRow = row.clone();
            generatedRows.add(clonedRow);

            for (Pair<Integer, Term> replacementValue : replacementValues) {
                Element originalValue = clonedRow.child(replacementValue.getKey());

                Element valueId = originalValue.clone();
                valueId.children().forEach(Node::remove);
                valueId.text(replacementValue.getValue().getId());
                clonedRow.appendChild(valueId);

                Element valueLabel = originalValue.clone();
                valueLabel.children().forEach(Node::remove);
                valueLabel.text(replacementValue.getValue().getName());
                clonedRow.appendChild(valueLabel);
            }

            row.before(clonedRow);
        }
        return generatedRows;
    }

    private static class NameTermSelector implements TermSelector {

        @Override
        public List<Term> select(Element tableData) {
            Elements elements = TableUtil.selectTaxonNames(tableData);
            return elements.stream().map(this::asTerm).collect(Collectors.toList());
        }

        @Override
        public String typeName() {
            return "taxon";
        }

        @Override
        public Term asTerm(Element element) {
            String obkms_id = element.attr("obkms_id");
            String label = element.text();
            return new TermImpl(TaxonomyProvider.OPEN_BIODIV.getIdPrefix() + obkms_id, label);
        }
    }

    private static class ReferenceTermSelector implements TermSelector {

        @Override
        public List<Term> select(Element columnIndex) {
            Elements elements = TableUtil.selectReferences(columnIndex);
            return elements.stream().map(this::asTerm).collect(Collectors.toList());
        }

        @Override
        public String typeName() {
            return "reference";
        }

        @Override
        public Term asTerm(Element element) {
            String rid = element.attr("rid");
            return new TermImpl(
                    "http://references.openbiodiv.net/" + rid,
                    element.text());
        }
    }
}
