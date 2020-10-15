package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.globalbioticinteractions.dataset.DatasetImpl;
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
        List<DatasetImporterForMetaTable.Column> columnNames = DatasetImporterForMetaTable.columnsFromExternalSchema(config.get("tableSchema"), new DatasetImpl(null, URI.create(baseUrl), inStream -> inStream));
        assertThat(columnNames.size(), is(40));
    }



    @Test
    public void generateSourceTaxon() throws IOException {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_CLASS, "class");
            }
        };
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("class"));
        properties.put(TaxonUtil.SOURCE_TAXON_ORDER, "order");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("order"));
        properties.put(TaxonUtil.SOURCE_TAXON_FAMILY, "family");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("family"));
        properties.put(TaxonUtil.SOURCE_TAXON_GENUS, "genus");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("genus"));
        properties.put(TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET, "species");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("genus species"));
        properties.put(TaxonUtil.SOURCE_TAXON_SUBSPECIFIC_EPITHET, "subspecies");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("genus species subspecies"));
    }

    @Test
    public void generateTargetTaxon() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_CLASS, "class");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("class"));
        properties.put(TaxonUtil.TARGET_TAXON_ORDER, "order");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("order"));
        properties.put(TaxonUtil.TARGET_TAXON_FAMILY, "family");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("family"));
        properties.put(TaxonUtil.TARGET_TAXON_GENUS, "genus");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("genus"));
        properties.put(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET, "species");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("genus species"));
        properties.put(TaxonUtil.TARGET_TAXON_SUBSPECIFIC_EPITHET, "subspecies");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("genus species subspecies"));
    }

    @Test
    public void generateTargetTaxonIgnoreEmptyGenus() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_GENUS, "");
                put(TaxonUtil.TARGET_TAXON_CLASS, "class");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("class"));
    }

    @Test
    public void generateTargetTaxonPath() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_GENUS, "genusValue");
                put(TaxonUtil.TARGET_TAXON_CLASS, "classValue");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonPath(properties), is("classValue | genusValue"));
        assertThat(TaxonUtil.generateTargetTaxonPathNames(properties), is("class | genus"));
    }

    @Test
    public void generateSourceTaxonPath() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_GENUS, "aGenus");
                put(TaxonUtil.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateSourceTaxonPath(properties), is("aClass | aGenus"));
        assertThat(TaxonUtil.generateSourceTaxonPathNames(properties), is("class | genus"));
    }

    @Test
    public void generateSourceTaxonPathWithSpecificEpithet() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET, "some epithet");
                put(TaxonUtil.SOURCE_TAXON_GENUS, "aGenus");
                put(TaxonUtil.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateSourceTaxonPath(properties), is("aClass | aGenus | aGenus some epithet"));
        assertThat(TaxonUtil.generateSourceTaxonPathNames(properties), is("class | genus | species"));
    }

    @Test
    public void generateTargetTaxonPathWithSpecificEpithet() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET, "some epithet");
                put(TaxonUtil.TARGET_TAXON_SPECIES, "some species");
                put(TaxonUtil.TARGET_TAXON_GENUS, "aGenus");
                put(TaxonUtil.TARGET_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonPath(properties), is("aClass | aGenus | aGenus some epithet"));
        assertThat(TaxonUtil.generateTargetTaxonPathNames(properties), is("class | genus | species"));
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("aGenus some epithet"));
    }

    @Test
    public void generateSourceTaxonPathWithSpecies() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_SPECIES, "some species");
                put(TaxonUtil.SOURCE_TAXON_GENUS, "aGenus");
                put(TaxonUtil.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateSourceTaxonPath(properties), is("aClass | aGenus | some species"));
        assertThat(TaxonUtil.generateSourceTaxonPathNames(properties), is("class | genus | species"));
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("some species"));
    }

    @Test
    public void generateTargetTaxonPathWithSpecies() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_SPECIES, "some species");
                put(TaxonUtil.TARGET_TAXON_GENUS, "aGenus");
                put(TaxonUtil.TARGET_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonPath(properties), is("aClass | aGenus | some species"));
        assertThat(TaxonUtil.generateTargetTaxonPathNames(properties), is("class | genus | species"));
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("some species"));
    }

    @Test
    public void generateSpeciesName() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_GENUS, "aGenus");
                put(TaxonUtil.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateSpeciesName(properties,
                TaxonUtil.SOURCE_TAXON_GENUS,
                TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET,
                TaxonUtil.SOURCE_TAXON_SUBSPECIFIC_EPITHET,
                TaxonUtil.SOURCE_TAXON_SPECIES), is(nullValue()));
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
    public void generateReferenceAndReferenceId() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(DatasetImporterForMetaTable.AUTHOR, "Johnny");
                put(DatasetImporterForMetaTable.TITLE, "My first pony");
                put(DatasetImporterForMetaTable.YEAR, "1981");
                put(DatasetImporterForMetaTable.JOURNAL, "journal of bla");
            }
        };

        assertThat(DatasetImporterForMetaTable.generateReferenceCitation(properties), is("Johnny, 1981. My first pony. journal of bla."));
        properties.put(DatasetImporterForMetaTable.VOLUME, "123");
        assertThat(DatasetImporterForMetaTable.generateReferenceCitation(properties), is("Johnny, 1981. My first pony. journal of bla, 123."));
        properties.put(DatasetImporterForMetaTable.NUMBER, "11");
        assertThat(DatasetImporterForMetaTable.generateReferenceCitation(properties), is("Johnny, 1981. My first pony. journal of bla, 123(11)."));
        properties.put(DatasetImporterForMetaTable.PAGES, "33");

        assertThat(DatasetImporterForMetaTable.generateReferenceCitation(properties), is("Johnny, 1981. My first pony. journal of bla, 123(11), pp.33."));

    }

    @Test
    public void phibaseColumnCount() throws IOException, StudyImporterException {
        final InputStream inputStream = DatasetImporterForMetaTable.class.getResourceAsStream("phi-base-schema.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        List<DatasetImporterForMetaTable.Column> columnNames = DatasetImporterForMetaTable.columnNamesForSchema(config);
        assertThat(columnNames.size(), is(86));

        DatasetImporterForMetaTable importer = new DatasetImporterForMetaTable(null, null);
        DatasetLocal dataset = new DatasetLocal(inStream -> inStream);

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
        DatasetLocal dataset = new DatasetLocal(inStream -> inStream) {
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
        DatasetLocal dataset = new DatasetLocal(inStream -> inStream);

        dataset.setConfig(config);
        importer.setDataset(dataset);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setInteractionListener(links::add);
        importer.importStudy();

        assertThat(links.size(), is(2));

        assertThat(links.get(0).get("empty1"), is("Homo sapiens"));
        assertThat(links.get(0).get("empty3"), is("eats: Canis lupus | eats: Catus felis"));

        assertThat(links.get(0).get(TaxonUtil.SOURCE_TAXON_NAME), is("Homo sapiens"));
        assertThat(links.get(0).get(DatasetImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(links.get(0).get(DatasetImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(links.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Canis lupus"));

        assertThat(links.get(1).get(TaxonUtil.SOURCE_TAXON_NAME), is("Homo sapiens"));
        assertThat(links.get(1).get(DatasetImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(links.get(1).get(DatasetImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(links.get(1).get(TaxonUtil.TARGET_TAXON_NAME), is("Catus felis"));
    }


}