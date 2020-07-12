package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.HttpUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class StudyImporterForPensoftTest {

    public static void parseRowsAndEnrich(JsonNode biodivTable, InteractionListener listener, ResourceService resourceService) throws StudyImporterException {
        StudyImporterForPensoft.parseRowsAndEnrich(biodivTable, listener, new NullImportLogger(), resourceService);
    }

    @Test
    public void generateSchema() throws IOException {
        Elements tables = StudyImporterForPensoft.getTables(getTableObj());

        List<String> columnNames = StudyImporterForPensoft.getColumnNames(tables);

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
    public void retrieveCitation() throws IOException {
        String citation = StudyImporterForPensoft.findCitationByDoi("10.3897/zookeys.306.5455", getResourceServiceTest());
        assertThat(citation, is("Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. http://openbiodiv.net/D37E8D1A-221B-FFA6-FFE7-4458FFA0FFC2. https://doi.org/10.3897/zookeys.306.5455"));
    }

    public ResourceService getResourceServiceTest() {
        return new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                HttpGet req = new HttpGet(resourceName);
                String csvString = HttpUtil.executeAndRelease(req, HttpUtil.getFailFastHttpClient());
                return IOUtils.toInputStream(csvString, StandardCharsets.UTF_8);
            }
        };
    }

    @Test
    public void retrieveTaxonFamily() throws IOException {
        Taxon taxon = StudyImporterForPensoft.retrieveTaxonHierarchyById("4B689A17-2541-4F5F-A896-6F0C2EEA3FB4", getResourceServiceTest());
        assertThat(taxon.getName(), is("Acanthaceae"));
        assertThat(taxon.getRank(), is("family"));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(taxon.getPath(), is("Plantae | Tracheophyta | Magnoliopsida | Lamiales | Acanthaceae"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family"));
    }

    @Test
    public void retrieveTaxonSpecies() throws IOException {
        Taxon taxon = StudyImporterForPensoft.retrieveTaxonHierarchyById("6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC", getResourceServiceTest());
        assertThat(taxon.getName(), is("Copidothrips octarticulatus"));
        assertThat(taxon.getRank(), is(nullValue()));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC"));
        assertThat(taxon.getPath(), is("Copidothrips octarticulatus"));
        assertThat(taxon.getPathNames(), is(""));
    }

    @Test
    public void retrieveTaxonSpecies2() throws IOException {
        Taxon taxon = StudyImporterForPensoft.retrieveTaxonHierarchyById("22A7F215-829B-458A-AEBB-39FFEA6D4A91", getResourceServiceTest());
        assertThat(taxon.getName(), is("Bolacothrips striatopennatus"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/22A7F215-829B-458A-AEBB-39FFEA6D4A91"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Thysanoptera | Thripidae | Bolacothrips | Bolacothrips striatopennatus"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | genus | species"));
    }

    @Test
    public void singleTableHostInColumnHeader() throws IOException, TermLookupServiceException, StudyImporterException {

        final JsonNode tableObj = getTableObj();

        assertNotNull(tableObj);

        tableObj.get("article_doi");
        final JsonNode annotations = tableObj.get("annotations");
        for (JsonNode annotation : annotations) {
            final InteractTypeMapper interactTypeMapper =
                    new InteractTypeMapperFactoryImpl().create();
            if (annotation.has("id")) {
                final InteractType interactType = interactTypeMapper.getInteractType(annotation.get("id").asText());
            }
            if (annotation.has("context")) {
                String verbatimInteraction = annotation.get("context").asText();
            }
            annotation.get("row");
            annotation.get("column");
            annotation.get("possition");


        }

        tableObj.get("caption");


        List<Map<String, String>> rowValues = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                rowValues.add(link);
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

    }

    @Test
    public void handleRowSpan() throws IOException, TermLookupServiceException, StudyImporterException {

        final InputStream resourceAsStream = getClass().getResourceAsStream("pensoft/rows-with-rowspan.html");
        final Document doc = Jsoup.parseBodyFragment(IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8));
        final Elements rows = doc.select("tr");


        final Element firstRow = rows.get(0);
        Elements rowColumns = firstRow.select("td");

        StudyImporterForPensoft.expandSpannedRows(firstRow, rowColumns);

        final TreeMap<String, String> rowValue = new TreeMap<>();

        StudyImporterForPensoft.parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue,
                rowColumns);

        assertThat(rowValue, hasEntry("one", "Apiaceae"));

        Elements cols2 = rows.get(1).select("td");


        final TreeMap<String, String> rowValue2 = new TreeMap<>();
        StudyImporterForPensoft.parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue2,
                cols2);

        assertThat(rowValue2, hasEntry("one", "Apiaceae"));

        Elements cols3 = rows.get(2).select("td");

        final TreeMap<String, String> rowValue3 = new TreeMap<>();
        StudyImporterForPensoft.parseRowValues(
                Arrays.asList("one", "two", "three"),
                rowValue3,
                cols3);

        assertThat(rowValue3, hasEntry("one", "Apocynaceae"));
    }

    @Test
    public void parseTableContent() throws IOException, TermLookupServiceException, StudyImporterException {

        final JsonNode tableObj = getTableObj();

        List<Map<String, String>> rowValues = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                rowValues.add(new TreeMap<>(link));
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

        assertThat(rowValues.size(), is(121));
        assertThat(rowValues.get(0), hasEntry("Family Name", "Acanthaceae"));
        assertThat(rowValues.get(0), hasEntry("Family Name_taxon_id", "http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(rowValues.get(0), hasEntry("Family Name_taxon_name", "Acanthaceae"));
        assertThat(rowValues.get(0), hasEntry("Host Plant", "Ruellia sp."));
        assertThat(rowValues.get(0), hasEntry("Host Plant_taxon_id", "http://openbiodiv.net/56F59D49-725E-4BF7-8A6D-1B1A7A721231"));
        assertThat(rowValues.get(0), hasEntry("Host Plant_taxon_name", "Ruellia"));
        assertThat(rowValues.get(0), hasEntry("Thrips species", "Copidothrips octarticulatus<br/> Thrips parvispinus"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_taxon_id", "http://openbiodiv.net/6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_taxon_name", "Copidothrips octarticulatus"));

        assertThat(rowValues.get(1), hasEntry("Family Name", "Acanthaceae"));
        assertThat(rowValues.get(1), hasEntry("Family Name_taxon_id", "http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(rowValues.get(1), hasEntry("Family Name_taxon_name", "Acanthaceae"));

    }

    @Test
    public void parseTableContentWithRowSpan() throws IOException, TermLookupServiceException, StudyImporterException {

        final String tableContent = IOUtils.toString(getClass().getResourceAsStream("pensoft/table-with-rowspan.html"), StandardCharsets.UTF_8);

        final ObjectNode tableObj = new ObjectMapper().createObjectNode();
        tableObj.put("table_id", "<http://openbiodiv.net/FB706B4E-BAC2-4432-AD28-48063E7753E4>");
        tableObj.put("caption", "a caption");
        tableObj.put("article_doi", "some/doi");
        tableObj.put("table_content", tableContent);

        List<Map<String, String>> rowValues = new ArrayList<>();
        InteractionListener listener = new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                rowValues.add(new TreeMap<>(link));
            }
        };

        parseRowsAndEnrich(tableObj, listener, getResourceServiceTest());

        assertThat(rowValues.size(), is(8));
        assertThat(rowValues.get(0), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(0), hasEntry("Thrips species_taxon_name", "Thrips parvispinus"));
        assertThat(rowValues.get(1), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(1), hasEntry("Thrips species_taxon_name", "Thrips nigropilosus"));
        assertThat(rowValues.get(2), hasEntry("Family Name", "Apiaceae"));
        assertThat(rowValues.get(2), hasEntry("Thrips species_taxon_name", "Thrips parvispinus"));
        assertThat(rowValues.get(7), hasEntry("Family Name", "Apocynaceae"));
        assertThat(rowValues.get(7), hasEntry("Thrips species_taxon_name", "Thrips malloti"));

    }

    public JsonNode getTableObj() throws IOException {
        final InputStream is = getClass().getResourceAsStream("pensoft/annotated-table.json");
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

        StudyImporterForPensoft.expandRows(termPairs, new TestPermutationListener(links), StudyImporterForPensoft.distinctKeys(termPairs));
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

        StudyImporterForPensoft.expandRows(termPairs, new TestPermutationListener(links), StudyImporterForPensoft.distinctKeys(termPairs));

        links.forEach(System.out::println);
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

        StudyImporterForPensoft.expandRows(termPairs, new TestPermutationListener(links), StudyImporterForPensoft.distinctKeys(termPairs));

        assertThat(links.size(), is(1));

        final Map<String, String> expectedItem1 = new TreeMap<String, String>() {{
            put("1", "a:a");
            put("2", "c:c");
            put("3", "e:e");
        }};

        assertThat(links, hasItem(expectedItem1));
    }

    private static class TestPermutationListener implements StudyImporterForPensoft.PermutationListener {

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