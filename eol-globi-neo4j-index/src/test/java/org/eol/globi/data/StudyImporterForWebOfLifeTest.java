package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForWebOfLifeTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, IOException {
        StudyImporterForWebOfLife importer = new StudyImporterForWebOfLife(null, nodeFactory);
        importer.setDataset(new DatasetLocal());
        importer.importNetworks("weboflife/web-of-life_2016-01-15_192434.zip", "Web of Life. " + CitationUtil.createLastAccessedString("http://www.web-of-life.es/"));
        resolveNames();

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        List<String> references = new ArrayList<>();
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("Web of Life. Accessed at <http://www.web-of-life.es/>"));
            references.add(allStudy.getCitation());
        }

        assertThat(references, hasItem("Arroyo, M.T.K., R. Primack & J.J. Armesto. 1982. Community studies in pollination ecology in the high temperate Andes of central Chile. I. Pollination mechanisms and altitudinal variation. Amer. J. Bot. 69:82-97."));
        assertThat(taxonIndex.findTaxonByName("Diplopterys pubipetala"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Juniperus communis"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Turdus torquatus"), is(notNullValue()));
    }

    @Test
    public void retrieveNetworkList() throws IOException {
        final List<String> networkNames = StudyImporterForWebOfLife.getNetworkNames(ResourceUtil.asInputStream(StudyImporterForWebOfLife.WEB_OF_LIFE_BASE_URL + "/networkslist.php?type=All&data=All"));

        assertThat(networkNames, hasItem("A_HP_002"));
        assertThat(networkNames.size() > 50, is(true));
    }

    @Test
    public void generateArchiveURL() {
        final List<String> networkNames = Arrays.asList("A_HP_002", "A_HP_003");
        String generatedArchiveURL = StudyImporterForWebOfLife.generateArchiveURL(networkNames);

        String expectedArchiveURL = "http://www.web-of-life.es/map_download_fast2.php?format=csv&networks=" + "A_HP_002,A_HP_003" + "&species=yes&type=All&data=All&speciesrange=&interactionsrange=&searchbox=&checked=false";

        assertThat(generatedArchiveURL, is(expectedArchiveURL));
    }


}

