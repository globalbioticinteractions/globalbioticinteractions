package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForMetaTableTest {

    @Test
    public void parseColumnNames() throws IOException {
        final InputStream inputStream = DatasetImporterForMetaTable.class.getResourceAsStream("test-meta-globi.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        List<DatasetImporterForMetaTable.Column> columnNames = DatasetImporterForMetaTable.columnNamesForMetaTable(config);
        assertThat(columnNames.size(), is(40));
        assertThat(columnNames.get(0).getDefaultValue(), is("some default interaction name"));
        assertThat(columnNames.get(1).getDefaultValue(), is(nullValue()));
    }

    @Test
    public void parseSchemaWithPrimaryAndForeignKeys() throws IOException, StudyImporterException {
        final InputStream inputStream = DatasetImporterForMetaTable.class.getResourceAsStream("test-meta-globi-primary-key.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        DatasetImpl dataset = new DatasetImpl("foo/bar", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return null;
            }
        }, URI.create("https://example.org"));
        dataset.setConfig(config);

        List<JsonNode> tables = DatasetImporterForMetaTable.collectTables(dataset);

        assertThat(tables.size(), is(3));

        List<DatasetImporterForMetaTable.Column> columnNames = DatasetImporterForMetaTable.columnNamesForMetaTable(tables.get(0));
        assertThat(columnNames.size(), is(2));
        assertThat(columnNames.get(0).getKeyReference(), is("referenceId"));
        assertThat(columnNames.get(0).getKeyType(), is("primary"));

        columnNames = DatasetImporterForMetaTable.columnNamesForMetaTable(tables.get(1));
        assertThat(columnNames.size(), is(3));
        assertThat(columnNames.get(0).getKeyReference(), is("taxonId"));
        assertThat(columnNames.get(0).getKeyType(), is("foreign"));
        assertThat(columnNames.get(1).getKeyReference(), is("taxonId"));
        assertThat(columnNames.get(1).getKeyType(), is("foreign"));
        assertThat(columnNames.get(2).getKeyReference(), is("referenceId"));
        assertThat(columnNames.get(2).getKeyType(), is("foreign"));
        assertThat(columnNames.get(2).getSeparator(), is(","));

        columnNames = DatasetImporterForMetaTable.columnNamesForMetaTable(tables.get(2));
        assertThat(columnNames.size(), is(8));
        assertThat(columnNames.get(0).getKeyReference(), is("taxonId"));
        assertThat(columnNames.get(0).getKeyType(), is("primary"));
    }

    @Test
    public void importRecordsWithPrimaryAndForeignKeys() throws IOException, StudyImporterException {
        final InputStream inputStream = DatasetImporterForMetaTable.class.getResourceAsStream("test-meta-globi-primary-key.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        DatasetImpl dataset = new DatasetImpl("foo/bar", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                Map<URI, String> resourceMap = new HashMap<URI, String>() {{
                    put(URI.create("xlsx:https://datadryad.org/stash/downloads/file_stream/3078242!/Reference"), "test-meta-globi-primary-key-reference.tsv");
                    put(URI.create("xlsx:https://datadryad.org/stash/downloads/file_stream/3078242!/Metaweb"), "test-meta-globi-primary-key-metaweb.tsv");
                    put(URI.create("xlsx:https://datadryad.org/stash/downloads/file_stream/3078242!/Node%20Taxonomy"), "test-meta-globi-primary-key-taxonomy.tsv");
                }};

                String testResource = resourceMap.get(resourceName);
                InputStream resourceAsStream = DatasetImporterForMetaTableTest.this.getClass().getResourceAsStream(testResource);
                assertNotNull("failed to find test resource [" + testResource + "]", resourceAsStream);
                return resourceAsStream;
            }
        }, URI.create("https://example.org"));
        dataset.setConfig(config);


        DatasetImporterForMetaTable importer = new DatasetImporterForMetaTable(null, null);
        importer.setDataset(dataset);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setInteractionListener(links::add);
        importer.importStudy();

        assertThat(links.size(), is(39));

        Map<String, String> first = links.get(10);

        assertThat(first.get("sourceTaxonId"), is("Abbottina"));
        assertThat(first.get("sourceTaxonFamilyName"), is("Cyprinidae"));
        assertThat(first.get("interactionTypeId"), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(first.get("interactionTypeName"), is("eats"));
        assertThat(first.get("targetTaxonId"), is("Aethaloptera"));
        assertThat(first.get("targetTaxonFamilyName"), is("Hydropsychidae"));
        assertThat(first.get("referenceId"), is("R332"));
        assertThat(first.get("referenceCitation"), is("Son, Y.-M. (2000) Population ecology of Abbottina springeri (Cyprinidae) in the Musimchon stream, Korea. Korean Journal of Ichthyology, 12, 186–191."));


    }

    @Test
    public void indexTablesWithReferencedPrimaryKeys() throws IOException, StudyImporterException {
        final InputStream inputStream = DatasetImporterForMetaTable.class.getResourceAsStream("test-meta-globi-primary-key.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        ResourceServiceLocal service = new ResourceServiceLocal(inStream -> inStream, DatasetImporterForMetaTableTest.class);
        DatasetLocal dataset = new DatasetLocal(
                service) {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                Map<URI, String> resourceMap = new HashMap<URI, String>() {{
                    put(URI.create("xlsx:https://datadryad.org/stash/downloads/file_stream/3078242!/Reference"), "test-meta-globi-primary-key-reference.tsv");
                    put(URI.create("xlsx:https://datadryad.org/stash/downloads/file_stream/3078242!/Metaweb"), "test-meta-globi-primary-key-metaweb.tsv");
                    put(URI.create("xlsx:https://datadryad.org/stash/downloads/file_stream/3078242!/Node%20Taxonomy"), "test-meta-globi-primary-key-taxonomy.tsv");
                }};

                String testResource = resourceMap.get(resourceName);
                InputStream resourceAsStream = DatasetImporterForMetaTableTest.this.getClass().getResourceAsStream(testResource);
                assertNotNull("failed to find test resource [" + testResource + "]", resourceAsStream);
                return resourceAsStream;
            }

        };

        dataset.setConfig(config);


        Map<String, Map<String, Map<String, String>>> indexedDependencies = DatasetImporterForMetaTable.indexDependencies(dataset, null);

        assertThat(indexedDependencies.size(), is(2));

        Map<String, Map<String, String>> taxonomy = indexedDependencies.get("taxonId");
        assertThat(taxonomy.size(), is(19));
        assertThat(taxonomy.get("Detritus").get("taxonId"), is("Detritus"));
        Map<String, Map<String, String>> references = indexedDependencies.get("referenceId");
        assertThat(references.size(), is(10));
        assertThat(references.get("R001").get("referenceId"), is("R001"));
        assertThat(references.get("R001").get("referenceCitation"), is("Elliott, J.M. (1973) The diel activity pattern, drifting and food of the leech Erpobdella octoculata (L.) (Hirudinea: Erpobdellidae) in a Lake District stream. The Journal of Animal Ecology, 42, 449"));
    }

    @Test
    public void importRecordsWithListValues() throws IOException, StudyImporterException {
        final InputStream inputStream = DatasetImporterForMetaTable.class.getResourceAsStream("test-meta-globi-separator.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        DatasetImpl dataset = new DatasetImpl("foo/bar", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                Map<URI, String> resourceMap = new HashMap<URI, String>() {{
                    put(URI.create("https://figshare.com/ndownloader/files/2196534"), "test-meta-globi-separator.csv");
                }};

                String testResource = resourceMap.get(resourceName);
                InputStream resourceAsStream = DatasetImporterForMetaTableTest.this.getClass().getResourceAsStream(testResource);
                assertNotNull("failed to find test resource [" + testResource + "]", resourceAsStream);
                return resourceAsStream;
            }
        }, URI.create("https://example.org"));
        dataset.setConfig(config);


        DatasetImporterForMetaTable importer = new DatasetImporterForMetaTable(null, null);
        importer.setDataset(dataset);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setInteractionListener(links::add);
        importer.importStudy();

        assertThat(links.size(), is(152));

        Map<String, String> sample1 = links.get(5);

        assertThat(sample1.get("sourceTaxonName"), is("abaca bunchy top virus"));
        assertThat(sample1.get("interactionTypeId"), is("http://purl.obolibrary.org/obo/RO_0002454"));
        assertThat(sample1.get("interactionTypeName"), is("hasHost"));
        assertThat(sample1.get("targetTaxonName"), is("musa textilis"));
        assertThat(sample1.get("referenceUrl"), is("https://www.ncbi.nlm.nih.gov/nuccore/145845923"));

        Map<String, String> sample2 = links.get(141);

        assertThat(sample2.get("sourceTaxonName"), is("abiotrophia defectiva"));
        assertThat(sample2.get("interactionTypeId"), is("http://purl.obolibrary.org/obo/RO_0002454"));
        assertThat(sample2.get("interactionTypeName"), is("hasHost"));
        assertThat(sample2.get("targetTaxonName"), is("homo sapiens"));
        assertThat(sample2.get("referenceUrl"), is("https://pubmed.ncbi.nlm.nih.gov/11095068"));

        Map<String, String> sample3 = links.get(151);

        assertThat(sample3.get("sourceTaxonName"), is("acanthamoeba astronyxis"));
        assertThat(sample3.get("interactionTypeId"), is("http://purl.obolibrary.org/obo/RO_0002454"));
        assertThat(sample3.get("interactionTypeName"), is("hasHost"));
        assertThat(sample3.get("targetTaxonName"), is("homo sapiens"));
        assertThat(sample3.get("referenceUrl"), is("https://pubmed.ncbi.nlm.nih.gov/507100"));
    }



    @Test
    public void parseColumnValues() {

        HashMap<String, String> mappedLine = new HashMap<>();
        mappedLine.put("some column", "some original malformed value");
        final List<String> msgs = doParse(mappedLine, "some malformed value");

        assertThat(mappedLine.get("some column"), is("some original malformed value"));

        assertThat(msgs.size(), is(1));
        assertThat(msgs.get(0), is("failed to parse value [some malformed value] from column [original column name] into column [some column] with datatype: {\"base\":\"date\",\"format\":\"MM/dd/YYYY\",\"id\":\"some data type id\"}"));

    }

    @Test
    public void parseColumnValuesWithValidDate() {

        HashMap<String, String> mappedLine = new HashMap<>();
        final List<String> msgs = doParse(mappedLine, "01/01/2019");

        assertThat(mappedLine.get("some column"), is("2019-01-01T00:00:00.000Z"));

        assertThat(msgs.size(), is(0));
    }

    public List<String> doParse(HashMap<String, String> mappedLine, String aValue) {
        DatasetImporterForMetaTable.Column column
                = new DatasetImporterForMetaTable.Column("some column", "some data type id");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("MM/dd/YYYY");
        column.setOriginalName("original column name");

        final List<String> msgs = new ArrayList<>();

        DatasetImporterForMetaTable.parseColumnValue(new ImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                msgs.add(message);
            }

            @Override
            public void info(LogContext ctx, String message) {

            }

            @Override
            public void severe(LogContext ctx, String message) {
            }
        }, mappedLine, aValue, column);
        return msgs;
    }

    @Test
    public void parseColumnNamesFromExternalSchema() throws IOException {
        assertExpectedColumnCount("test-meta-globi-external-schema.json");
    }

    @Test
    public void parseColumnNamesFromExternalSchemaNoDatatype() throws IOException {
        assertExpectedColumnCount("test-meta-globi-external-schema-no-types.json");
    }

    @Test
    public void parseColumnNamesFromDefaultExternalSchema() throws IOException {
        assertExpectedColumnCount("test-meta-globi-default-external-schema.json");
    }

    public void assertExpectedColumnCount(String metaTableDef) throws IOException {
        final Class<DatasetImporterForMetaTable> clazz = DatasetImporterForMetaTable.class;
        final URL resource = clazz.getResource(metaTableDef);
        assertNotNull(resource);

        final InputStream inputStream = clazz.getResourceAsStream(metaTableDef);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        String baseUrl = resource.toExternalForm().replaceFirst(metaTableDef + "$", "");
        List<DatasetImporterForMetaTable.Column> columnNames = DatasetImporterForMetaTable.columnsFromExternalSchema(config.get("tableSchema"), new DatasetWithResourceMapping(null, URI.create(baseUrl), new ResourceServiceLocalAndRemote(inStream -> inStream)));
        assertThat(columnNames.size(), is(40));
    }


    @Test
    public void interactionTypeMapping() {
        InteractTypeMapper mapper = new InteractTypeMapper() {
            @Override
            public boolean shouldIgnoreInteractionType(String nameOrId) {
                return false;
            }

            @Override
            public InteractType getInteractType(String nameOrId) {
                return null;
            }
        };
        assertThat(DatasetImporterForMetaTable.generateInteractionType(interactMap("donald"), mapper), is(nullValue()));
    }

    @Test
    public void interactionTypeMappingValid() {
        InteractTypeMapper mapper = new InteractTypeMapper() {
            @Override
            public boolean shouldIgnoreInteractionType(String nameOrId) {
                return false;
            }

            @Override
            public InteractType getInteractType(String nameOrId) {
                return InteractType.POLLINATES;
            }
        };
        assertThat(DatasetImporterForMetaTable.generateInteractionType(interactMap("pollinator"), mapper), is(InteractType.POLLINATES));

    }

    @Test
    public void valueOrDefault() {
        assertThat(DatasetImporterForMetaTable.valueOrDefault(null, new DatasetImporterForMetaTable.Column("foo", "bar")), is(nullValue()));
        final DatasetImporterForMetaTable.Column columnWithDefault = new DatasetImporterForMetaTable.Column("foo", "bar");
        columnWithDefault.setDefaultValue("bla");
        assertThat(DatasetImporterForMetaTable.valueOrDefault(null, columnWithDefault), is("bla"));
        assertThat(DatasetImporterForMetaTable.valueOrDefault("boo", columnWithDefault), is("boo"));
    }

    @Test
    public void parseValue() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "bar");
        final String parsedValue = DatasetImporterForMetaTable.parseValue(null, column);
        assertThat(parsedValue, is(nullValue()));
    }

    @Test
    public void parseValueEOLTaxonId() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://eol.org/schema/taxonID");
        final String parsedValue = DatasetImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("EOL:123"));
    }

    @Test
    public void parseValueLongEOLTaxonId() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", null);
        column.setDataTypeBase("long");
        column.setValueUrl("http://eol.org/pages/{foo}");
        final String parsedValue = DatasetImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("EOL:123"));
    }

    @Test
    public void parseValueLongEOLTaxonIdMalformed() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", null);
        column.setDataTypeBase("long");
        column.setValueUrl("http://eol.org/pages/{foo}");
        final String parsedValue = DatasetImporterForMetaTable.parseValue("notANumber", column);
        assertThat(parsedValue, is(nullValue()));
    }

    @Test
    public void parseValueEOLTaxonIdNull() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://eol.org/schema/taxonID");
        final String parsedValue = DatasetImporterForMetaTable.parseValue(null, column);
        assertThat(parsedValue, is(nullValue()));
    }

    @Test
    public void parseValueNODC() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "https://marinemetadata.org/references/nodctaxacodes");
        final String parsedValue = DatasetImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("NODC:123"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseValueNODCNonNumeric() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "https://marinemetadata.org/references/nodctaxacodes");
        final String parsedValue = DatasetImporterForMetaTable.parseValue("a", column);
        assertThat(parsedValue, is(nullValue()));
    }

    @Test
    public void parseValueNCBI() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "string");
        column.setValueUrl("http://purl.obolibrary.org/obo/NCBITaxon_{foo}");
        final String parsedValue = DatasetImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("NCBI:123"));
    }

    @Test
    public void parseValueEventTime() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("dd-MMM-YY");
        final String parsedValue = DatasetImporterForMetaTable.parseValue("01-May-18", column);
        assertThat(parsedValue, is("2018-05-01T00:00:00.000Z"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseValueInvalidEventTime() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("dd-MMM-YY");
        DatasetImporterForMetaTable.parseValue("10-11 May-18", column);
    }


    @Test
    public void handleDateType() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("MM/dd/YYYY");
        String parsedString = DatasetImporterForMetaTable.handleDateType("02/01/1929", column);
        assertThat(parsedString, is("1929-02-01T00:00:00.000Z"));
    }

    @Test
    public void handleUSNMIxodesDateType() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("YYYYMMdd");
        String parsedString = DatasetImporterForMetaTable.handleDateType("1899----", column);
        assertThat(parsedString, is("1899-01-01T00:00:00.000Z"));
    }

    @Test
    public void handleUSNMIxodesDateType2() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("YYYYMMdd");
        String parsedString = DatasetImporterForMetaTable.handleDateType("197212--", column);
        assertThat(parsedString, is("1972-12-01T00:00:00.000Z"));
    }

    @Test
    public void handleDateTypeTruncated() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("MM/dd/YYYY");
        String parsedString = DatasetImporterForMetaTable.handleDateType("1929", column);
        assertThat(parsedString, is("1929-01-01T00:00:00.000Z"));
    }

    @Test
    public void handleDateTypeTruncated2() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("MM/dd/YYYY");
        String parsedString = DatasetImporterForMetaTable.handleDateType("03/1929", column);
        assertThat(parsedString, is("1929-03-01T00:00:00.000Z"));
    }

    @Test
    public void parseValueEOL() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "string");
        column.setValueUrl("http://eol.org/pages/{foo}");
        final String parsedValue = DatasetImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("EOL:123"));
    }

    @Test
    public void parseValueNODCNull() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("foo", "https://marinemetadata.org/references/nodctaxacodes");
        final String parsedValue = DatasetImporterForMetaTable.parseValue(null, column);
        assertThat(parsedValue, is(nullValue()));
    }

    public HashMap<String, String> interactMap(final String donald) {
        return new HashMap<String, String>() {
            {
                put(DatasetImporterForTSV.INTERACTION_TYPE_NAME, donald);
            }
        };
    }


    @Test
    public void generateReferenceUrl() {
        final DatasetImporterForMetaTable.Column column = new DatasetImporterForMetaTable.Column("referenceUrl", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setOriginalName("pmid");
        column.setDataTypeBase("string");
        column.setValueUrl("https://www.ncbi.nlm.nih.gov/pubmed/{referenceUrl}");
        String parsedString = DatasetImporterForMetaTable.parseValue("123", column);
        assertThat(parsedString, is("https://www.ncbi.nlm.nih.gov/pubmed/123"));

    }

    @Test
    public void phibaseColumnCount() throws IOException, StudyImporterException {
        final InputStream inputStream = DatasetImporterForMetaTable.class.getResourceAsStream("phi-base-schema.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        List<DatasetImporterForMetaTable.Column> columnNames = DatasetImporterForMetaTable.columnNamesForSchema(config);
        assertThat(columnNames.size(), is(86));

        DatasetImporterForMetaTable importer = new DatasetImporterForMetaTable(null, null);

        DatasetLocal dataset = new DatasetLocal(new ResourceServiceLocal(inStream -> inStream, DatasetImporterForMetaTableTest.class));

        JsonNode phibaseConfig = new ObjectMapper().readTree("{\n" +
                "  \"@context\": [\"http://www.w3.org/ns/csvw\", {\"@language\": \"en\"}],\n" +
                "  \"rdfs:comment\": [\"inspired by https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/\"],\n" +
                "  \"tables\": [\n" +
                "    { \"url\": \"phi-base_current-head.csv\",\n" +
                "      \"dcterms:bibliographicCitation\": \"Urban M, Cuzick A, Rutherford K, Irvine A, Pedro H, Pant R, Sadanadan V, Khamari L, Billal S, Mohanty S, Hammond-Kosack KE. PHI-base: a new interface and further additions for the multi-species pathogen-host interactions database. Nucleic Acids Res. 2017 Jan 4;45(D1):D604-D610. doi: 10.1093/nar/gkw1089. Epub 2016 Dec 3. PMID:27915230\",\n" +
                "      \"tableSchema\": \"phi-base-schema.json\",\n" +
                "      \"headerRowCount\": 1,\n" +
                "      \"interactionTypeId\": \"http://purl.obolibrary.org/obo/RO_0002556\",\n" +
                "      \"interactionTypeName\": \"pathogenOf\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");

        dataset.setConfig(phibaseConfig);
        importer.setDataset(dataset);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setInteractionListener(links::add);
        importer.importStudy();

        assertThat(links.size(), is(9));

        assertThat(links.get(1).get(TaxonUtil.SOURCE_TAXON_ID), is("NCBI:5499"));
        assertThat(links.get(1).get(DatasetImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002556"));
        assertThat(links.get(1).get(TaxonUtil.TARGET_TAXON_ID), is("NCBI:4081"));
    }

    @Test
    public void explicitNullValueForCatalogNumberUMMZI() throws IOException, StudyImporterException {
        DatasetImporterForMetaTable importer = new DatasetImporterForMetaTable(null, null);
        DatasetLocal dataset = new DatasetLocal(
                new ResourceServiceLocal(inStream -> inStream, DatasetImporterForMetaTableTest.class)) {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                Map<URI, String> resourceMap = new HashMap<URI, String>() {{
                    put(URI.create("interaction_types_ignored.csv"), "field_observation_id\nshouldBeIgnored");
                    put(URI.create("interaction_types_mapping.csv"), "observation_field_name,observation_field_id,interaction_type_label,interaction_type_id\n" +
                            "associated with,,interactsWith, http://purl.obolibrary.org/obo/RO_0002437");
                }};

                String input = resourceMap.get(resourceName);
                return StringUtils.isBlank(input)
                        ? super.retrieve(resourceName)
                        : IOUtils.toInputStream(input, StandardCharsets.UTF_8);
            }

        };

        JsonNode ummziConfig = new ObjectMapper().readTree(getClass().getResourceAsStream("ummzi-globi.json"));

        dataset.setConfig(ummziConfig);
        importer.setDataset(dataset);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setInteractionListener(links::add);
        importer.importStudy();

        assertThat(links.size(), is(20));

        assertThat(links.get(1).get(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER), is(nullValue()));
        assertThat(links.get(1).get(DatasetImporterForTSV.TARGET_CATALOG_NUMBER), is(nullValue()));

        assertThat(links.get(19).get(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER), is("`UMMZI-180800"));
        assertThat(links.get(19).get(DatasetImporterForTSV.TARGET_CATALOG_NUMBER), is("OC 43283"));

    }


    @Test
    public void associatedTaxa() throws IOException, StudyImporterException {
        final InputStream inputStream = getClass().getResourceAsStream("example-associated-taxa.json");
        assertNotNull(inputStream);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        DatasetImporterForMetaTable importer = new DatasetImporterForMetaTable(null, null);
        DatasetLocal dataset = new DatasetLocal(
                new ResourceServiceLocal(inStream -> inStream, this.getClass())
        );

        dataset.setConfig(config);
        importer.setDataset(dataset);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setInteractionListener(links::add);
        importer.importStudy();

        assertThat(links.size(), is(2));

        assertThat(links.get(0).get("empty1"), is("Homo sapiens"));
        assertThat(links.get(0).get("empty3"), is("eats: Canis lupus | eats: Catus felis"));

        assertThat(links.get(0).get(TaxonUtil.SOURCE_TAXON_NAME), is("Homo sapiens"));
        assertThat(links.get(0).get(DatasetImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(links.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Canis lupus"));

        assertThat(links.get(1).get(TaxonUtil.SOURCE_TAXON_NAME), is("Homo sapiens"));
        assertThat(links.get(1).get(DatasetImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(links.get(1).get(TaxonUtil.TARGET_TAXON_NAME), is("Catus felis"));
    }


}