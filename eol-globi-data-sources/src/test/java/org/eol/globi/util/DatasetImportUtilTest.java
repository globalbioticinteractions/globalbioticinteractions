package org.eol.globi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.data.StudyImporterException;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetProxy;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImportUtilTest {

    public static List<Dataset> getReferences(Dataset dataset) {
        List<Dataset> referenceList = new ArrayList<>();
        final JsonNode config = dataset.getConfig();
        final JsonNode references = config.get("references");
        if (references != null && references.isArray()) {
            for (JsonNode reference : references) {
                final DatasetProxy datasetProxy = new DatasetProxy(dataset);
                datasetProxy.setConfig(reference);
                referenceList.add(datasetProxy);
            }

        }
        return referenceList;
    }

    @Test
    public void listDependencies() throws StudyImporterException, IOException {

        String jsonConfig = "{" +
                "  \"format\": \"dwca\", " +
                "  \"citation\": \"The MSB Division of Parasitology Collection\"," +
                "  \"url\": \"http://ipt.vertnet.org:8080/ipt/archive.do?r=msb_para\"," +
                "  \"references\": [" +
                "    {" +
                "      \"format\": \"dwca\"," +
                "      \"url\": \"http://ipt.vertnet.org:8080/ipt/archive.do?r=msb_host\"," +
                "      \"citation\": \"The MSB Division of Host Collection\"" +
                "    }, {" +
                "      \"format\": \"dwca\"," +
                "      \"url\": \"http://ipt.vertnet.org:8080/ipt/archive.do?r=msb_host2\"," +
                "      \"citation\": \"The MSB Division of Host Collection2\"" +
                "    }]" +
                "}";

        final DatasetImpl datasetOrig = new DatasetWithResourceMapping(
                "name/space",
                URI.create("some:uri"),
                new ResourceServiceLocal(in -> in)
        );
        JsonNode objectNode = new ObjectMapper().readTree(jsonConfig);

        datasetOrig.setConfig(objectNode);
        final List<Dataset> references = getReferences(datasetOrig);

        assertThat(references.size(), Is.is(2));
    }


}