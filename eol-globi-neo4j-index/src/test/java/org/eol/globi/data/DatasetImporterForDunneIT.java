package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForDunneIT extends GraphDBNeo4jTestCase {

    @Test
    public void importStudy() throws StudyImporterException, IOException {
        String configJson = "{ \"citation\": \"blabla\",\n" +
                "  \"format\": \"dunne\",\n" +
                "  \"resources\": {\n" +
                "    \"nodes\": \"https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakIntertidal/main/nodes.tsv\",\n" +
                "    \"links\": \"https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakIntertidal/main/links.tsv\"\n" +
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
        DatasetImpl dunne2016 = new DatasetWithResourceMapping("dunne2016", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), getCacheDir()));
        dunne2016.setConfig(jsonNode);
        ParserFactory parserFactory = new ParserFactoryForDataset(dunne2016);
        DatasetImporterForDunne importer = new DatasetImporterForDunne(parserFactory, nodeFactory);
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
                "    \"nodes\": \"https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakNearshore/main/nodes.tsv\",\n" +
                "    \"links\": \"https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakNearshore/main/links.tsv\"\n" +
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
        DatasetImpl dunne2016 = new DatasetWithResourceMapping("dunne2016", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), getCacheDir()));
        dunne2016.setConfig(new ObjectMapper().readTree(configJson));
        ParserFactory parserFactory = new ParserFactoryForDataset(dunne2016);
        DatasetImporterForDunne importer = new DatasetImporterForDunne(parserFactory, nodeFactory);
        importer.setDataset(dunne2016);

        importStudy(importer);

        StudyNode study = getStudySingleton(getGraphDb());

        assertThat(study, is(notNullValue()));

        assertThat(study.getCitation(), is(notNullValue()));

        assertThat(getSpecimenCount(study), is(6774 * 2));

    }

}