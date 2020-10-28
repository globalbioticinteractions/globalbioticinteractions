package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForCoetzerTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, IOException {
        DatasetImporterForCoetzer importer = new DatasetImporterForCoetzer(null, nodeFactory);
        DatasetImpl dataset = new DatasetLocal(inStream -> inStream);
        JsonNode config = new ObjectMapper().readTree("{\"citation\": \"source citation\", \"resources\": {\"archive\": \"coetzer/CatalogOfAfrotropicalBees.zip\"}}");
        dataset.setConfig(config);
        importer.setDataset(dataset);
        importStudy(importer);

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getCitation(), is(notNullValue()));
        }

        assertThat(taxonIndex.findTaxonByName("Agrostis tremula"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Coelioxys erythrura"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Patellapis namaquensis"), is(notNullValue()));

    }

}
