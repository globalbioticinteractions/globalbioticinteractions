package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.service.ResourceService;
import org.eol.globi.tool.NullImportLogger;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.DatasetImporterForPensoft.createColumnSchema;
import static org.eol.globi.data.DatasetImporterForPensoft.expandSpannedRows;
import static org.eol.globi.data.DatasetImporterForPensoft.extractTermsForRowValue;
import static org.eol.globi.data.DatasetImporterForPensoft.getColumnNames;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

public class DatasetImporterForPensoftTest {

    public static void parseRowsAndEnrich(JsonNode biodivTable, InteractionListener listener, ResourceService resourceService) throws StudyImporterException {
        DatasetImporterForPensoft.parseRowsAndEnrich(biodivTable, listener, new NullImportLogger(), new OpenBiodivClient(resourceService));
    }

    @Test
    public void generateSchema() throws IOException {
        final JsonNode table_content = getTableObj().get("table_content");
        final String html = table_content.asText();
        final Document doc = Jsoup.parse(html);

        Elements tables = doc.select("table");

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
    public void generateTableData() throws IOException, StudyImporterException {
        List<Map<String, String>> links = new ArrayList<>();

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

        assertThat(links.size(), is(1350));

        for (Map<String, String> link : links) {
            link.forEach((x, y) ->
                    System.out.println("[" + x + "]: [" + StringUtils.abbreviate(y, 80) + "]")
            );
            assertThat(link.get("tableSchema"), is("{\"columns\":[{\"name\":\"Family Name\",\"titles\":\"Family Name\",\"datatype\":\"string\"},{\"name\":\"Host Plant\",\"titles\":\"Host Plant\",\"datatype\":\"string\"},{\"name\":\"Thrips species\",\"titles\":\"Thrips species\",\"datatype\":\"string\"},{\"name\":\"Family Name_expanded_taxon_id\",\"titles\":\"Family Name_expanded_taxon_id\",\"datatype\":\"string\"},{\"name\":\"Family Name_expanded_taxon_name\",\"titles\":\"Family Name_expanded_taxon_name\",\"datatype\":\"string\"},{\"name\":\"Host Plant_expanded_taxon_id\",\"titles\":\"Host Plant_expanded_taxon_id\",\"datatype\":\"string\"},{\"name\":\"Host Plant_expanded_taxon_name\",\"titles\":\"Host Plant_expanded_taxon_name\",\"datatype\":\"string\"},{\"name\":\"Thrips species_expanded_taxon_id\",\"titles\":\"Thrips species_expanded_taxon_id\",\"datatype\":\"string\"},{\"name\":\"Thrips species_expanded_taxon_name\",\"titles\":\"Thrips species_expanded_taxon_name\",\"datatype\":\"string\"}]}"));
        }



    }


    @Test
    public void handleRowSpan() throws IOException {

        final InputStream resourceAsStream = getClass().getResourceAsStream("/org/eol/globi/data/pensoft/rows-with-rowspan.html");
        final Document doc = Jsoup.parseBodyFragment(IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8));
        final Elements rows = doc.select("tr");


        final Element firstRow = rows.get(0);
        Elements rowColumns = firstRow.select("td");

        expandSpannedRows(firstRow, rowColumns);

        final TreeMap<String, String> rowValue = new TreeMap<>();

        extractTermsForRowValue(
                Arrays.asList("one", "two", "three"),
                rowValue,
                rowColumns);

        assertThat(rowValue, hasEntry("one", "Apiaceae"));

        Elements cols2 = rows.get(1).select("td");


        final TreeMap<String, String> rowValue2 = new TreeMap<>();
        extractTermsForRowValue(
                Arrays.asList("one", "two", "three"),
                rowValue2,
                cols2);

        assertThat(rowValue2, hasEntry("one", "Apiaceae"));

        Elements cols3 = rows.get(2).select("td");

        final TreeMap<String, String> rowValue3 = new TreeMap<>();
        extractTermsForRowValue(
                Arrays.asList("one", "two", "three"),
                rowValue3,
                cols3);

        assertThat(rowValue3, hasEntry("one", "Apocynaceae"));
    }

    public static JsonNode getTableObj() throws IOException {
        return getTableObj("/org/eol/globi/data/pensoft/annotated-table.json");
    }

    public static JsonNode getTableObj(String jsonString) throws IOException {
        final InputStream is = DatasetImporterForPensoftTest
                .class
                .getResourceAsStream(jsonString);

        return new ObjectMapper()
                .readTree(is);
    }


}