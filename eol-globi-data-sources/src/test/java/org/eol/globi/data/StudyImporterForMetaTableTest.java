package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class StudyImporterForMetaTableTest {

    @Test
    public void parseColumnNames() throws IOException, StudyImporterException {
        final InputStream inputStream = StudyImporterForMetaTable.class.getResourceAsStream("test-meta-globi.json");
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        List<StudyImporterForMetaTable.Column> columnNames = StudyImporterForMetaTable.columnNamesForMetaTable(config);
        assertThat(columnNames.size(), is(40));
        assertThat(columnNames.get(0).getDefaultValue(), is("some default interaction name"));
        assertThat(columnNames.get(1).getDefaultValue(), is(nullValue()));
    }

    @Test
    public void parseColumnNamesFromExternalSchema() throws IOException, StudyImporterException {
        assertExpectedColumnCount("test-meta-globi-external-schema.json");
    }

    @Test
    public void parseColumnNamesFromDefaultExternalSchema() throws IOException, StudyImporterException {
        assertExpectedColumnCount("test-meta-globi-default-external-schema.json");
    }

    public void assertExpectedColumnCount(String metaTableDef) throws IOException {
        final Class<StudyImporterForMetaTable> clazz = StudyImporterForMetaTable.class;
        final URL resource = clazz.getResource(metaTableDef);
        assertNotNull(resource);

        final InputStream inputStream = clazz.getResourceAsStream(metaTableDef);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        String baseUrl = resource.toExternalForm().replaceFirst(metaTableDef + "$", "");
        List<StudyImporterForMetaTable.Column> columnNames = StudyImporterForMetaTable.columnsFromExternalSchema(config.get("tableSchema"), new DatasetImpl(null, URI.create(baseUrl)));
        assertThat(columnNames.size(), is(40));
    }



    @Test
    public void generateSourceTaxon() throws IOException, StudyImporterException {
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
    public void generateTargetTaxon() throws IOException, StudyImporterException {
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


}