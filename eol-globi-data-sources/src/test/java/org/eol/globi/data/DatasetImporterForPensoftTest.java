package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.tool.NullImportLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

public class DatasetImporterForPensoftTest {

    public static void parseRowsAndEnrich(JsonNode biodivTable, InteractionListener listener, ResourceService resourceService) throws StudyImporterException {
        DatasetImporterForPensoft.parseRowsAndEnrich(biodivTable, listener, new NullImportLogger(), new OpenBiodivClient(resourceService));
    }

    @Test
    public void generateSchema() throws IOException {
        Elements tables = DatasetImporterForPensoft.getTables(getTableObj());

        List<String> columnNames = DatasetImporterForPensoft.getColumnNames(tables);

        final ObjectNode obj = createColumnSchema(columnNames);

        final ObjectMapper objectMapper = new ObjectMapper();

        final String tableSchema = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);

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
    public void handleRowSpan() throws IOException {

        final InputStream resourceAsStream = getClass().getResourceAsStream("pensoft/rows-with-rowspan.html");
        final Document doc = Jsoup.parseBodyFragment(IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8));
        final Elements rows = doc.select("tr");


        final Element firstRow = rows.get(0);
        Elements rowColumns = firstRow.select("td");

        DatasetImporterForPensoft.expandSpannedRows(firstRow, rowColumns);

        final TreeMap<String, String> rowValue = new TreeMap<>();

        DatasetImporterForPensoft.parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue,
                rowColumns);

        assertThat(rowValue, hasEntry("one", "Apiaceae"));

        Elements cols2 = rows.get(1).select("td");


        final TreeMap<String, String> rowValue2 = new TreeMap<>();
        DatasetImporterForPensoft.parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue2,
                cols2);

        assertThat(rowValue2, hasEntry("one", "Apiaceae"));

        Elements cols3 = rows.get(2).select("td");

        final TreeMap<String, String> rowValue3 = new TreeMap<>();
        DatasetImporterForPensoft.parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue3,
                cols3);

        assertThat(rowValue3, hasEntry("one", "Apocynaceae"));
    }



    public static JsonNode getTableObj() throws IOException {
        final InputStream is = DatasetImporterForPensoftTest.class.getResourceAsStream("pensoft/annotated-table.json");
        return new ObjectMapper().readTree(is);
    }

    public ObjectNode createColumnSchema(List<String> columnNames) {
        final List<ObjectNode> columns = columnNames
                .stream()
                .map(x -> {
                    final ObjectMapper objectMapper = new ObjectMapper();
                    final ObjectNode column = objectMapper.createObjectNode();
                    column.put("name", x);
                    column.put("titles", x);
                    column.put("datatype", "string");
                    return column;
                }).collect(Collectors.toList());

        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode obj = objectMapper.createObjectNode();
        final ArrayNode arrayNode = objectMapper.createArrayNode();
        columns.forEach(arrayNode::add);
        obj.put("columns", arrayNode);
        return obj;
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

        DatasetImporterForPensoft.expandRows(termPairs, new TestPermutationListener(links), DatasetImporterForPensoft.distinctKeys(termPairs));
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

        DatasetImporterForPensoft.expandRows(termPairs, new TestPermutationListener(links), DatasetImporterForPensoft.distinctKeys(termPairs));

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

        DatasetImporterForPensoft.expandRows(termPairs, new TestPermutationListener(links), DatasetImporterForPensoft.distinctKeys(termPairs));

        assertThat(links.size(), is(1));

        final Map<String, String> expectedItem1 = new TreeMap<String, String>() {{
            put("1", "a:a");
            put("2", "c:c");
            put("3", "e:e");
        }};

        assertThat(links, hasItem(expectedItem1));
    }

    private static class TestPermutationListener implements DatasetImporterForPensoft.PermutationListener {

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