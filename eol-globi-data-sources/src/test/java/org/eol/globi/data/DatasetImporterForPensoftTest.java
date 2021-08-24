package org.eol.globi.data;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.ResourceService;
import org.eol.globi.tool.NullImportLogger;
import org.globalbioticinteractions.util.OpenBiodivClientImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForPensoft.createColumnSchema;
import static org.eol.globi.data.DatasetImporterForPensoft.getColumnNames;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DatasetImporterForPensoftTest {

    public static void parseRowsAndEnrich(JsonNode biodivTable, InteractionListener listener, ResourceService resourceService) throws StudyImporterException {
        DatasetImporterForPensoft datasetImporterForPensoft = new DatasetImporterForPensoft(null, null);
        datasetImporterForPensoft.parseRowsAndEnrich(biodivTable, listener, new NullImportLogger(), new OpenBiodivClientImpl(resourceService));
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
            public void on(Map<String, String> interaction) throws StudyImporterException {
                links.add(interaction);
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
            assertThat(link.get("tableSchema"), is("{\"columns\":[{\"name\":\"Family Name\",\"titles\":\"Family Name\",\"datatype\":\"string\"},{\"name\":\"Host Plant\",\"titles\":\"Host Plant\",\"datatype\":\"string\"},{\"name\":\"Thrips species\",\"titles\":\"Thrips species\",\"datatype\":\"string\"},{\"name\":\"Family Name_expanded_taxon_id\",\"titles\":\"Family Name_expanded_taxon_id\",\"datatype\":\"string\"},{\"name\":\"Family Name_expanded_taxon_name\",\"titles\":\"Family Name_expanded_taxon_name\",\"datatype\":\"string\"},{\"name\":\"Host Plant_expanded_taxon_id\",\"titles\":\"Host Plant_expanded_taxon_id\",\"datatype\":\"string\"},{\"name\":\"Host Plant_expanded_taxon_name\",\"titles\":\"Host Plant_expanded_taxon_name\",\"datatype\":\"string\"},{\"name\":\"Thrips species_expanded_taxon_id\",\"titles\":\"Thrips species_expanded_taxon_id\",\"datatype\":\"string\"},{\"name\":\"Thrips species_expanded_taxon_name\",\"titles\":\"Thrips species_expanded_taxon_name\",\"datatype\":\"string\"}]}"));
        }

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

    @Test
    public void extractInteractionType() throws IOException {
        String jsonSnippet = "{\"annotations\":[{\"id\":[\"http://purl.obolibrary.org/obo/RO_0002453\",\"http://purl.obolibrary.org/obo/RO_0002453\"],\"lbl\":[\"host\",\"host\"],\"length\":[4.0,4.0],\"possition\":[0.0,0.0],\"ontology\":[\"custom\",\"custom\"],\"type\":[\"PROPERTY\",\"PROPERTY\"],\"context\":[\"host plant\",\"host plant\"],\"is_synonym\":[true,true],\"is_word\":[true,true],\"row\":[\"1\"],\"col\":[\"2\"]}]}";

        String interactionType = DatasetImporterForPensoft.parseInteractionType(new ObjectMapper().readTree(jsonSnippet));

        assertThat(interactionType, is("http://purl.obolibrary.org/obo/RO_0002453"));

    }


}