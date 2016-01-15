package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class StudyImporterForMetaTableTest {

    @Test
    public void parseColumnNames() throws IOException, StudyImporterException {
        final InputStream inputStream = ResourceUtil.asInputStream("test-meta-globi.json", StudyImporterForMetaTable.class);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        List<String> columnNames = StudyImporterForMetaTable.columnNamesForMetaTable(config);
        assertThat(columnNames.size(), is(40));

    }

    @Test
    public void generateSourceCitation() throws IOException, StudyImporterException {
        final InputStream inputStream = ResourceUtil.asInputStream("test-meta-globi.json", StudyImporterForMetaTable.class);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        String citation = StudyImporterForMetaTable.generateSourceCitation("http://base", config);
        assertThat(citation, startsWith("Seltzer, Carrie; Wysocki, William; Palacios, Melissa; Eickhoff, Anna; Pilla, Hannah; Aungst, Jordan; Mercer, Aaron; Quicho, Jamie; Voss, Neil; Xu, Man; J. Ndangalasi, Henry; C. Lovett, Jon; J. Cordeiro, Norbert (2015): Plant-animal interactions from Africa. figshare. https://dx.doi.org/10.6084/m9.figshare.1526128 . Accessed at https://ndownloader.figshare.com/files/2231424"));
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