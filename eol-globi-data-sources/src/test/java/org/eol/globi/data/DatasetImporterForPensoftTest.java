package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.tool.NullImportLogger;
import org.globalbioticinteractions.pensoft.ExpandColumnSpans;
import org.globalbioticinteractions.pensoft.ExpandRowSpans;
import org.globalbioticinteractions.pensoft.ExpandRowValues;
import org.globalbioticinteractions.pensoft.TableProcessor;
import org.globalbioticinteractions.pensoft.TablePreprocessor;
import org.globalbioticinteractions.pensoft.TableRectifier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.DatasetImporterForPensoft.PermutationListener;
import static org.eol.globi.data.DatasetImporterForPensoft.createColumnSchema;
import static org.eol.globi.data.DatasetImporterForPensoft.distinctKeys;
import static org.eol.globi.data.DatasetImporterForPensoft.expandRows;
import static org.eol.globi.data.DatasetImporterForPensoft.expandSpannedColumns;
import static org.eol.globi.data.DatasetImporterForPensoft.expandSpannedRows;
import static org.eol.globi.data.DatasetImporterForPensoft.getColumnNames;
import static org.eol.globi.data.DatasetImporterForPensoft.getHtmlTable;
import static org.eol.globi.data.DatasetImporterForPensoft.parseRowValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

public class DatasetImporterForPensoftTest {

    public static void parseRowsAndEnrich(JsonNode biodivTable, InteractionListener listener, ResourceService resourceService) throws StudyImporterException {
        DatasetImporterForPensoft.parseRowsAndEnrich(biodivTable, listener, new NullImportLogger(), new OpenBiodivClient(resourceService));
    }

    @Test
    public void generateSchema() throws IOException {
        Elements tables = getHtmlTable(getTableObj());

        List<String> columnNames = getColumnNames(tables);

        final ObjectNode obj = createColumnSchema(columnNames);

        final ObjectMapper objectMapper = new ObjectMapper();

        final String tableSchema = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(obj);

        assertThat(tableSchema, is("{\n" +
                "  \"columns\" : [ {\n" +
                "    \"name\" : \"Family Name\",\n" +
                "    \"titles\" : \"Family Name\",\n" +
                "    \"datatype\" : \"string\"\n" +
                "  }, {\n" +
                "    \"name\" : \"Host Plant\",\n" +
                "    \"titles\" : \"Host Plant\",\n" +
                "    \"datatype\" : \"string\"\n" +
                "  }, {\n" +
                "    \"name\" : \"Thrips species\",\n" +
                "    \"titles\" : \"Thrips species\",\n" +
                "    \"datatype\" : \"string\"\n" +
                "  } ]\n" +
                "}"));
    }

    @Test
    public void rectifyTable() throws IOException {
        TableProcessor rectifier = new TableRectifier();
        String inputString = IOUtils.toString(getClass().getResourceAsStream("pensoft/annotated-table-provided.html"), CharsetConstant.UTF8);
        String processed = rectifier.process(inputString);

        assertThat(processed, is(IOUtils.toString(getClass().getResourceAsStream("pensoft/annotated-table-expanded-row-values.html"), CharsetConstant.UTF8)));
    }

    @Test
    public void prepTable() throws IOException {
        String inputString = IOUtils.toString(getClass().getResourceAsStream("pensoft/annotated-table-provided.html"), CharsetConstant.UTF8);
        TableProcessor prep = new TablePreprocessor();
        String processedString = prep.process(inputString);
        assertThat(processedString,
                is(IOUtils.toString(getClass().getResourceAsStream("pensoft/annotated-table-preprocessed.html"), CharsetConstant.UTF8)));
    }

    @Test
    public void doExpandRows() throws IOException {
        String preppedTable = IOUtils
                .toString(getClass()
                        .getResourceAsStream("pensoft/annotated-table-preprocessed.html"), CharsetConstant.UTF8);

        assertThat(preppedTable, containsString("rowspan=\"2\""));

        TableProcessor prep = new ExpandRowSpans();


        String processedString = prep.process(preppedTable);
        assertThat(processedString, not(containsString("rowspan=\"2\"")));

        assertThat(processedString,
                is(IOUtils.toString(getClass().getResourceAsStream("pensoft/annotated-table-expanded-rows.html"), CharsetConstant.UTF8)));
    }

    @Test
    public void doExpandColumnSpans() throws IOException {
        String preppedTable = IOUtils
                .toString(getClass()
                        .getResourceAsStream("pensoft/table-with-colspan-zookeys.318.5693.html"), CharsetConstant.UTF8);

        assertThat(preppedTable, containsString("colspan=\"6\""));

        TableProcessor prep = new ExpandColumnSpans();

        String processedString = prep.process(preppedTable);

        assertThat(processedString, not(containsString("colspan=\"6\"")));

    }

    @Test
    public void rectifyTableWithColumnSpans() throws IOException {
        String preppedTable = IOUtils
                .toString(getClass()
                        .getResourceAsStream("pensoft/table-with-colspan-zookeys.318.5693.html"), CharsetConstant.UTF8);

        assertThat(preppedTable, containsString("colspan=\"6\""));

        TableProcessor prep = new TableRectifier();

        String processedString = prep.process(preppedTable);

        assertThat(processedString, not(containsString("colspan=\"6\"")));

        assertThat(processedString,
                is(IOUtils.toString(getClass().getResourceAsStream("pensoft/table-with-colspan-zookeys.318.5693-colspan-expanded.html"), CharsetConstant.UTF8)));

    }

    @Test
    public void doExpandValueLists() throws IOException {
        String preppedTable = IOUtils
                .toString(getClass()
                        .getResourceAsStream("pensoft/annotated-table-expanded-rows.html"), CharsetConstant.UTF8);

        TableProcessor prep = new ExpandRowValues();


        String processedString = prep.process(preppedTable);
        System.out.println(processedString);

        assertThat(processedString,
                is(IOUtils.toString(getClass().getResourceAsStream("pensoft/annotated-table-expanded-row-values.html"), CharsetConstant.UTF8)));
    }


    @Test
    public void generateTableData() throws IOException, StudyImporterException {
        List<Map<String, String>> links = new ArrayList<>();
        Elements tables = getHtmlTable(getTableObj());

        parseRowsAndEnrich(getTableObj(), new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                links.add(link);
            }
        }, new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                String journalQueryPrefix = "http://graph.openbiodiv.net/repositories/OpenBiodiv2020?query=PREFIX%20fabio:%20%3Chttp://purl.org/spar/fabio/%3E%0APREFIX%20prism:%20%3Chttp://prismstandard.org/namespaces/basic/2.0/%3E%0APREFIX%20doco:%20%3Chttp://purl.org/spar/doco/%3E%0APREFIX%20dc:%20%3Chttp://purl.org/dc/elements/1.1/%3E%0ASELECT%20%20%20%20?article%20%20%20%20?title%20%20%20%20?doi%20%20%20%20(group_concat(distinct%20?authorName;%20separator=%22,%20%22)%20as%20?authorsList)%20%20%20%20%20(%20REPLACE(str(?pubDate),%20%22(%5C%5Cd*)-.*%22,%20%22$1%22)%20as%20?pubYear)%20%20%20%20?journalName%20WHERE%20%7B%20%0A%20%20%20%20BIND(";
                String taxonQueryPrefix = "http://graph.openbiodiv.net/repositories/OpenBiodiv2020?query=PREFIX%20fabio:%20%3Chttp://purl.org/spar/fabio/%3E%0APREFIX%20prism:%20%3Chttp://prismstandard.org/namespaces/basic/2.0/%3E%0APREFIX%20doco:%20%3Chttp://purl.org/spar/doco/%3E%0APREFIX%20dc:%20%3Chttp://purl.org/dc/elements/1.1/%3E%0Aselect%20?name%20?rank%20?id%20?kingdom%20?phylum%20?class%20?order%20?family%20?genus%20?specificEpithet%20where%20%7B%20%7B%0A%20%20%20%20BIND";
                if (StringUtils.startsWith(resourceName.toString(), journalQueryPrefix)) {
                    return getClass().getResourceAsStream("/org/eol/globi/data/pensoft/pensoft-sparql-result.txt");
                } else if (resourceName.toString().startsWith(taxonQueryPrefix)) {
                    return getClass().getResourceAsStream("/org/eol/globi/data/pensoft/pensoft-sparql-result2.txt");
                } else {
                    throw new RuntimeException("unexpected");
                }
            }
        });

        assertThat(links.size(), is(121));

        for (Map<String, String> link : links) {
            link.forEach((x, y) ->
                    System.out.println("[" + x + "]: [" + y + "]"));
        }

    }


    @Test
    public void handleRowSpan() throws IOException {

        final InputStream resourceAsStream = getClass().getResourceAsStream("pensoft/rows-with-rowspan.html");
        final Document doc = Jsoup.parseBodyFragment(IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8));
        final Elements rows = doc.select("tr");


        final Element firstRow = rows.get(0);
        Elements rowColumns = firstRow.select("td");

        expandSpannedRows(firstRow, rowColumns);

        final TreeMap<String, String> rowValue = new TreeMap<>();

        parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue,
                rowColumns);

        assertThat(rowValue, hasEntry("one", "Apiaceae"));

        Elements cols2 = rows.get(1).select("td");


        final TreeMap<String, String> rowValue2 = new TreeMap<>();
        parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue2,
                cols2);

        assertThat(rowValue2, hasEntry("one", "Apiaceae"));

        Elements cols3 = rows.get(2).select("td");

        final TreeMap<String, String> rowValue3 = new TreeMap<>();
        parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue3,
                cols3);

        assertThat(rowValue3, hasEntry("one", "Apocynaceae"));
    }

    @Test
    public void handleRowSpanZookeys_318_5693() throws IOException {

        final InputStream resourceAsStream = getClass().getResourceAsStream("pensoft/table-with-colspan-zookeys.318.5693.html");
        final Document doc = Jsoup.parseBodyFragment(IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8));
        final Elements rows = doc.select("tr");

        List<String> columnNames = getColumnNames(rows);

        assertThat(columnNames, is(Arrays.asList(
                "Aphidura species Host plant",
                "Country",
                "Locality",
                "Date",
                "Coll.",
                "Sample"))
        );

        Map<String, String> rowContext = expandSpannedColumns(
                columnNames,
                rows.get(1));

        assertThat(rowContext.size(), is(1));

        assertThat(rowContext.get("Aphidura species Host plant"),
                is("Aphidura acanthophylli"));

    }

    public static JsonNode getTableObj() throws IOException {
        return getTableObj("pensoft/annotated-table.json");
    }

    public static JsonNode getTableObj(String jsonString) throws IOException {
        final InputStream is = DatasetImporterForPensoftTest
                .class
                .getResourceAsStream(jsonString);

        return new ObjectMapper()
                .readTree(is);
    }

    @Test
    public void permute() throws StudyImporterException {

        List<Map<String, String>> links = new ArrayList<>();
        final List<Pair<String, Term>> termPairs = Arrays.asList(
                Pair.of("1", asTerm("a")),
                Pair.of("1", asTerm("b")),
                Pair.of("2", asTerm("c")),
                Pair.of("2", asTerm("d")),
                Pair.of("3", asTerm("e"))
        );

        expandRows(termPairs, new TestPermutationListener(links), distinctKeys(termPairs));
        assertThat(links.size(), is(4));

        final Map<String, String> expectedItem1 = new TreeMap<String, String>() {{
            put("1", "a:a");
            put("2", "c:c");
            put("3", "e:e");
        }};
        final Map<String, String> expectedItem2 = new TreeMap<String, String>() {{
            put("1", "a:a");
            put("2", "d:d");
            put("3", "e:e");
        }};
        final Map<String, String> expectedItem3 = new TreeMap<String, String>() {{
            put("1", "b:b");
            put("2", "c:c");
            put("3", "e:e");
        }};
        final Map<String, String> expectedItem4 = new TreeMap<String, String>() {{
            put("1", "b:b");
            put("2", "d:d");
            put("3", "e:e");
        }};
        assertThat(links, hasItems(expectedItem1, expectedItem2, expectedItem3, expectedItem4));
    }

    @Test
    public void permute3() throws StudyImporterException {

        List<Map<String, String>> links = new ArrayList<>();
        final List<Pair<String, Term>> termPairs = Arrays.asList(
                Pair.of("1", asTerm("a")),
                Pair.of("1", asTerm("b")),
                Pair.of("2", asTerm("c")),
                Pair.of("2", asTerm("d")),
                Pair.of("3", asTerm("e"))
        );

        expandRows(termPairs, new TestPermutationListener(links), distinctKeys(termPairs));

        assertThat(links.size(), is(4));

        final Map<String, String> expectedItem1 = new TreeMap<String, String>() {{
            put("1", "a:a");
            put("2", "c:c");
            put("3", "e:e");
        }};
        final Map<String, String> expectedItem2 = new TreeMap<String, String>() {{
            put("1", "a:a");
            put("2", "d:d");
            put("3", "e:e");
        }};
        final Map<String, String> expectedItem3 = new TreeMap<String, String>() {{
            put("1", "b:b");
            put("2", "c:c");
            put("3", "e:e");
        }};
        final Map<String, String> expectedItem4 = new TreeMap<String, String>() {{
            put("1", "b:b");
            put("2", "d:d");
            put("3", "e:e");
        }};
        assertThat(links, hasItems(expectedItem1, expectedItem2, expectedItem3, expectedItem4));
    }

    public TermImpl asTerm(String id) {
        return new TermImpl(id, id);
    }


    @Test
    public void permute2() throws StudyImporterException {

        List<Map<String, String>> links = new ArrayList<>();
        final List<Pair<String, Term>> termPairs = Arrays.asList(
                Pair.of("1", asTerm("a")),
                Pair.of("2", asTerm("c")),
                Pair.of("3", asTerm("e"))
        );

        expandRows(termPairs, new TestPermutationListener(links), distinctKeys(termPairs));

        assertThat(links.size(), is(1));

        final Map<String, String> expectedItem1 = new TreeMap<String, String>() {{
            put("1", "a:a");
            put("2", "c:c");
            put("3", "e:e");
        }};

        assertThat(links, hasItem(expectedItem1));
    }

    private static class TestPermutationListener implements PermutationListener {

        private final List<Map<String, String>> links;
        private final List<Map<String, Term>> linksObj;

        public TestPermutationListener(List<Map<String, String>> links) {
            this.links = links;
            this.linksObj = new ArrayList<>();
        }

        @Override
        public void on(Map<String, Term> link) {
            if (!linksObj.contains(link)) {
                Map<String, String> translated = new HashMap<>();
                for (String s : link.keySet()) {
                    translated.put(s, link.get(s).getId() + ":" + link.get(s).getName());
                }
                links.add(translated);
                linksObj.add(link);
            }
        }
    }

}