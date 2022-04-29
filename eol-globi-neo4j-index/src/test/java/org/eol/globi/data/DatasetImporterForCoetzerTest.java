package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.ResourceServiceLocal;
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
        DatasetImpl dataset = new DatasetLocal(new ResourceServiceLocal(inStream -> inStream, getClass()));
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
