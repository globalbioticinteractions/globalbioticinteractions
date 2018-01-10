package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Study;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class StudyImporterForCoetzerTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, IOException {
        StudyImporterForCoetzer importer = new StudyImporterForCoetzer(null, nodeFactory);
        DatasetImpl dataset = new DatasetLocal();
        JsonNode config = new ObjectMapper().readTree("{\"citation\": \"source citation\", \"resources\": {\"archive\": \"coetzer/CatalogOfAfrotropicalBees.zip\"}}");
        dataset.setConfig(config);
        importer.setDataset(dataset);
        importStudy(importer);

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("source citation"));
            assertThat(allStudy.getSource(), containsString("Accessed at"));
        }

        assertThat(taxonIndex.findTaxonByName("Agrostis tremula"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Coelioxys erythrura"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Patellapis namaquensis"), is(notNullValue()));

    }

}
