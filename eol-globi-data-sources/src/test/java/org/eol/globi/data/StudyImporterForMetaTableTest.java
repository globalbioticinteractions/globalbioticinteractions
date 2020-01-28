package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.service.DatasetLocal;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class StudyImporterForMetaTableTest {

    @Test
    public void parseColumnNames() throws IOException {
        final InputStream inputStream = StudyImporterForMetaTable.class.getResourceAsStream("test-meta-globi.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        List<StudyImporterForMetaTable.Column> columnNames = StudyImporterForMetaTable.columnNamesForMetaTable(config);
        assertThat(columnNames.size(), is(40));
        assertThat(columnNames.get(0).getDefaultValue(), is("some default interaction name"));
        assertThat(columnNames.get(1).getDefaultValue(), is(nullValue()));
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
        final Class<StudyImporterForMetaTable> clazz = StudyImporterForMetaTable.class;
        final URL resource = clazz.getResource(metaTableDef);
        assertNotNull(resource);

        final InputStream inputStream = clazz.getResourceAsStream(metaTableDef);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        String baseUrl = resource.toExternalForm().replaceFirst(metaTableDef + "$", "");
        List<StudyImporterForMetaTable.Column> columnNames = StudyImporterForMetaTable.columnsFromExternalSchema(config.get("tableSchema"), new DatasetImpl(null, URI.create(baseUrl), inStream -> inStream));
        assertThat(columnNames.size(), is(40));
    }



    @Test
    public void generateSourceTaxon() throws IOException {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(StudyImporterForMetaTable.SOURCE_TAXON_CLASS, "class");
            }
        };
        assertThat(StudyImporterForMetaTable.generateSourceTaxonName(properties), is("class"));
        properties.put(StudyImporterForMetaTable.SOURCE_TAXON_ORDER, "order");
        assertThat(StudyImporterForMetaTable.generateSourceTaxonName(properties), is("order"));
        properties.put(StudyImporterForMetaTable.SOURCE_TAXON_FAMILY, "family");
        assertThat(StudyImporterForMetaTable.generateSourceTaxonName(properties), is("family"));
        properties.put(StudyImporterForMetaTable.SOURCE_TAXON_GENUS, "genus");
        assertThat(StudyImporterForMetaTable.generateSourceTaxonName(properties), is("genus"));
        properties.put(StudyImporterForMetaTable.SOURCE_TAXON_SPECIFIC_EPITHET, "species");
        assertThat(StudyImporterForMetaTable.generateSourceTaxonName(properties), is("genus species"));
        properties.put(StudyImporterForMetaTable.SOURCE_TAXON_SUBSPECIFIC_EPITHET, "subspecies");
        assertThat(StudyImporterForMetaTable.generateSourceTaxonName(properties), is("genus species subspecies"));
    }

    @Test
    public void generateTargetTaxon() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(StudyImporterForMetaTable.TARGET_TAXON_CLASS, "class");
            }
        };
        assertThat(StudyImporterForMetaTable.generateTargetTaxonName(properties), is("class"));
        properties.put(StudyImporterForMetaTable.TARGET_TAXON_ORDER, "order");
        assertThat(StudyImporterForMetaTable.generateTargetTaxonName(properties), is("order"));
        properties.put(StudyImporterForMetaTable.TARGET_TAXON_FAMILY, "family");
        assertThat(StudyImporterForMetaTable.generateTargetTaxonName(properties), is("family"));
        properties.put(StudyImporterForMetaTable.TARGET_TAXON_GENUS, "genus");
        assertThat(StudyImporterForMetaTable.generateTargetTaxonName(properties), is("genus"));
        properties.put(StudyImporterForMetaTable.TARGET_TAXON_SPECIFIC_EPITHET, "species");
        assertThat(StudyImporterForMetaTable.generateTargetTaxonName(properties), is("genus species"));
        properties.put(StudyImporterForMetaTable.TARGET_TAXON_SUBSPECIFIC_EPITHET, "subspecies");
        assertThat(StudyImporterForMetaTable.generateTargetTaxonName(properties), is("genus species subspecies"));
    }

    @Test
    public void generateTargetTaxonIgnoreEmptyGenus() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(StudyImporterForMetaTable.TARGET_TAXON_GENUS, "");
                put(StudyImporterForMetaTable.TARGET_TAXON_CLASS, "class");
            }
        };
        assertThat(StudyImporterForMetaTable.generateTargetTaxonName(properties), is("class"));
    }

    @Test
    public void generateTargetTaxonPath() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(StudyImporterForMetaTable.TARGET_TAXON_GENUS, "genusValue");
                put(StudyImporterForMetaTable.TARGET_TAXON_CLASS, "classValue");
            }
        };
        assertThat(StudyImporterForMetaTable.generateTargetTaxonPath(properties), is("classValue | genusValue"));
        assertThat(StudyImporterForMetaTable.generateTargetTaxonPathNames(properties), is("class | genus"));
    }

    @Test
    public void generateSourceTaxonPath() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(StudyImporterForMetaTable.SOURCE_TAXON_GENUS, "aGenus");
                put(StudyImporterForMetaTable.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(StudyImporterForMetaTable.generateSourceTaxonPath(properties), is("aClass | aGenus"));
        assertThat(StudyImporterForMetaTable.generateSourceTaxonPathNames(properties), is("class | genus"));
    }

    @Test
    public void interactionTypeMapping() {
        assertThat(StudyImporterForMetaTable.generateInteractionType(interactMap("donald")), is(nullValue()));
        assertThat(StudyImporterForMetaTable.generateInteractionType(interactMap("pollinator")), is(InteractType.POLLINATES));
    }

    @Test
    public void valueOrDefault() {
        assertThat(StudyImporterForMetaTable.valueOrDefault(null, new StudyImporterForMetaTable.Column("foo", "bar")), is(nullValue()));
        final StudyImporterForMetaTable.Column columnWithDefault = new StudyImporterForMetaTable.Column("foo", "bar");
        columnWithDefault.setDefaultValue("bla");
        assertThat(StudyImporterForMetaTable.valueOrDefault(null, columnWithDefault), is("bla"));
        assertThat(StudyImporterForMetaTable.valueOrDefault("boo", columnWithDefault), is("boo"));
    }

    @Test
    public void parseValue() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "bar");
        final String parsedValue = StudyImporterForMetaTable.parseValue(null, column);
        assertThat(parsedValue, is(nullValue()));
    }

    @Test
    public void parseValueEOLTaxonId() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "http://eol.org/schema/taxonID");
        final String parsedValue = StudyImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("EOL:123"));
    }

    @Test
    public void parseValueLongEOLTaxonId() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", null);
        column.setDataTypeBase("long");
        column.setValueUrl("http://eol.org/pages/{foo}");
        final String parsedValue = StudyImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("EOL:123"));
    }

    @Test
    public void parseValueLongEOLTaxonIdMalformed() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", null);
        column.setDataTypeBase("long");
        column.setValueUrl("http://eol.org/pages/{foo}");
        final String parsedValue = StudyImporterForMetaTable.parseValue("notANumber", column);
        assertThat(parsedValue, is(nullValue()));
    }

    @Test
    public void parseValueEOLTaxonIdNull() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "http://eol.org/schema/taxonID");
        final String parsedValue = StudyImporterForMetaTable.parseValue(null, column);
        assertThat(parsedValue, is(nullValue()));
    }

    @Test
    public void parseValueNODC() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "https://marinemetadata.org/references/nodctaxacodes");
        final String parsedValue = StudyImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("NODC:123"));
    }

    @Test
    public void parseValueNCBI() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "string");
        column.setValueUrl("http://purl.obolibrary.org/obo/NCBITaxon_{foo}");
        final String parsedValue = StudyImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("NCBITaxon:123"));
    }

    @Test
    public void parseValueEventTime() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("dd-MMM-YY");
        final String parsedValue = StudyImporterForMetaTable.parseValue("01-May-18", column);
        assertThat(parsedValue, is("2018-05-01T00:00:00.000Z"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseValueInvalidEventTime() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "http://rs.tdwg.org/dwc/terms/eventDate");
        column.setDataTypeBase("date");
        column.setDataTypeFormat("dd-MMM-YY");
        StudyImporterForMetaTable.parseValue("10-11 May-18", column);
    }

    @Test
    public void parseValueEOL() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "string");
        column.setValueUrl("http://eol.org/pages/{foo}");
        final String parsedValue = StudyImporterForMetaTable.parseValue("123", column);
        assertThat(parsedValue, is("EOL:123"));
    }

    @Test
    public void parseValueNODCNull() {
        final StudyImporterForMetaTable.Column column = new StudyImporterForMetaTable.Column("foo", "https://marinemetadata.org/references/nodctaxacodes");
        final String parsedValue = StudyImporterForMetaTable.parseValue(null, column);
        assertThat(parsedValue, is(nullValue()));
    }

    public HashMap<String, String> interactMap(final String donald) {
        return new HashMap<String, String>() {
            {
                put(StudyImporterForTSV.INTERACTION_TYPE_NAME, donald);
            }
        };
    }

    @Test
    public void generateReferenceAndReferenceId() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(StudyImporterForMetaTable.AUTHOR, "Johnny");
                put(StudyImporterForMetaTable.TITLE, "My first pony");
                put(StudyImporterForMetaTable.YEAR, "1981");
                put(StudyImporterForMetaTable.JOURNAL, "journal of bla");
            }
        };

        assertThat(StudyImporterForMetaTable.generateReferenceCitation(properties), is("Johnny, 1981. My first pony. journal of bla."));
        properties.put(StudyImporterForMetaTable.VOLUME, "123");
        assertThat(StudyImporterForMetaTable.generateReferenceCitation(properties), is("Johnny, 1981. My first pony. journal of bla, 123."));
        properties.put(StudyImporterForMetaTable.NUMBER, "11");
        assertThat(StudyImporterForMetaTable.generateReferenceCitation(properties), is("Johnny, 1981. My first pony. journal of bla, 123(11)."));
        properties.put(StudyImporterForMetaTable.PAGES, "33");

        assertThat(StudyImporterForMetaTable.generateReferenceCitation(properties), is("Johnny, 1981. My first pony. journal of bla, 123(11), pp.33."));

    }

    @Test
    public void phibaseColumnCount() throws IOException, StudyImporterException {
        final InputStream inputStream = StudyImporterForMetaTable.class.getResourceAsStream("phi-base-schema.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        List<StudyImporterForMetaTable.Column> columnNames = StudyImporterForMetaTable.columnNamesForSchema(config);
        assertThat(columnNames.size(), is(86));

        StudyImporterForMetaTable importer = new StudyImporterForMetaTable(null, null);
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

        assertThat(links.get(1).get(StudyImporterForTSV.SOURCE_TAXON_ID), is("NCBITaxon:5499"));
        assertThat(links.get(1).get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002556"));
        assertThat(links.get(1).get(StudyImporterForTSV.TARGET_TAXON_ID), is("NCBITaxon:4081"));
    }


    @Test
    public void associatedTaxa() throws IOException, StudyImporterException {
        final InputStream inputStream = getClass().getResourceAsStream("example-associated-taxa.json");
        assertNotNull(inputStream);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        StudyImporterForMetaTable importer = new StudyImporterForMetaTable(null, null);
        DatasetLocal dataset = new DatasetLocal(inStream -> inStream);

        dataset.setConfig(config);
        importer.setDataset(dataset);
        List<Map<String, String>> links = new ArrayList<>();

        importer.setInteractionListener(links::add);
        importer.importStudy();

        assertThat(links.size(), is(2));

        assertThat(links.get(0).get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Homo sapiens"));
        assertThat(links.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(links.get(0).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(links.get(0).get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Canis lupus"));

        assertThat(links.get(1).get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Homo sapiens"));
        assertThat(links.get(1).get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(links.get(1).get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(links.get(1).get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Catus felis"));
    }


}