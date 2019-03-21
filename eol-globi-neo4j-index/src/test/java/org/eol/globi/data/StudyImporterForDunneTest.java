package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForDunneTest extends GraphDBTestCase {

    @Test
    public void importStudy() throws StudyImporterException, IOException {
        String configJson = "{ \"citation\": \"blabla\",\n" +
                "  \"format\": \"dunne\",\n" +
                "  \"resources\": {\n" +
                "    \"nodes\": \"https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakIntertidal/master/nodes.tsv\",\n" +
                "    \"links\": \"https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakIntertidal/master/links.tsv\"\n" +
                "  },\n" +
                "  \"location\": {\n" +
                "    \"locality\": {\n" +
                "      \"id\": \"GEONAMES:5873327\",\n" +
                "      \"name\": \"Sanak Island, Alaska, USA\"\n" +
                "    },\n" +
                "    \"latitude\": 60,\n" +
                "    \"longitude\": 60\n" +
                "  }\n" +
                "}";
        JsonNode jsonNode = new ObjectMapper().readTree(configJson);
        DatasetImpl dunne2016 = new DatasetImpl("dunne2016", URI.create("http://example.com"));
        dunne2016.setConfig(jsonNode);
        ParserFactory parserFactory = new ParserFactoryForDataset(dunne2016);
        StudyImporterForDunne importer = new StudyImporterForDunne(parserFactory, nodeFactory);
        importer.setDataset(dunne2016);

        importStudy(importer);

        StudyNode study = getStudySingleton(getGraphDb());

        assertThat(study, is(notNullValue()));

        assertThat(getSpecimenCount(study), is(1804 * 2));

    }

    @Test
    public void importStudy2() throws StudyImporterException, IOException {

        String configJson = "{ \"citation\": \"blabla\",\n" +
                "  \"format\": \"dunne\",\n" +
                "  \"resources\": {\n" +
                "    \"nodes\": \"https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakNearshore/master/nodes.tsv\",\n" +
                "    \"links\": \"https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakNearshore/master/links.tsv\"\n" +
                "  },\n" +
                "  \"location\": {\n" +
                "    \"locality\": {\n" +
                "      \"id\": \"GEONAMES:5873327\",\n" +
                "      \"name\": \"Sanak Island, Alaska, USA\"\n" +
                "    },\n" +
                "    \"latitude\": 60,\n" +
                "    \"longitude\": 60\n" +
                "  }\n" +
                "}";
        DatasetImpl dunne2016 = new DatasetImpl("dunne2016", URI.create("http://example.com"));
        dunne2016.setConfig(new ObjectMapper().readTree(configJson));
        ParserFactory parserFactory = new ParserFactoryForDataset(dunne2016);
        StudyImporterForDunne importer = new StudyImporterForDunne(parserFactory, nodeFactory);
        importer.setDataset(dunne2016);

        importStudy(importer);

        StudyNode study = getStudySingleton(getGraphDb());

        assertThat(study, is(notNullValue()));

        assertThat(study.getSource(), containsString("Accessed at "));

        assertThat(getSpecimenCount(study), is(6774 * 2));

    }

}