package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.Dataset;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForDunneTest extends GraphDBTestCase {

    @Test
    public void importStudy() throws StudyImporterException, IOException {
        StudyImporterForDunne importer = new StudyImporterForDunne(new ParserFactoryImpl(), nodeFactory);

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
        Dataset dunne2016 = new Dataset("dunne2016", URI.create("http://example.com"));
        dunne2016.setConfig(jsonNode);
        importer.setDataset(dunne2016);

        Study study = importStudy(importer);

        assertThat(study, is(notNullValue()));

        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }
        assertThat(count, is(1804 * 2));

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
        StudyImporterForDunne importer = new StudyImporterForDunne(new ParserFactoryImpl(), nodeFactory);
        Dataset dunne2016 = new Dataset("dunne2016", URI.create("http://example.com"));
        dunne2016.setConfig(new ObjectMapper().readTree(configJson));
        importer.setDataset(dunne2016);

        Study study = importStudy(importer);
        assertThat(study, is(notNullValue()));

        assertThat(study.getSource(), containsString("Accessed at "));

        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }
        assertThat(count, is(6774 * 2));

    }

}