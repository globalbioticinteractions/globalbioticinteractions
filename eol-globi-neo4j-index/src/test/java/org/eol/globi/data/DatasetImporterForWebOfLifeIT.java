package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForWebOfLifeIT extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException {
        DatasetImporterForWebOfLife importer = new DatasetImporterForWebOfLife(null, nodeFactory);
        importer.setDataset(new DatasetLocal(inStream -> inStream));
        importer.importNetworks(URI.create("weboflife/web-of-life_2016-01-15_192434.zip"), "Web of Life. " + CitationUtil.createLastAccessedString("http://www.web-of-life.es/"));
        resolveNames();

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        List<String> references = new ArrayList<>();
        for (Study allStudy : allStudies) {
            references.add(allStudy.getCitation());
        }

        assertThat(references, hasItem("Arroyo, M.T.K., R. Primack & J.J. Armesto. 1982. Community studies in pollination ecology in the high temperate Andes of central Chile. I. Pollination mechanisms and altitudinal variation. Amer. J. Bot. 69:82-97."));
        assertThat(taxonIndex.findTaxonByName("Diplopterys pubipetala"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Juniperus communis"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Turdus torquatus"), is(notNullValue()));
    }

    @Test
    public void retrieveNetworkList() throws IOException {
        final List<URI> networkNames = DatasetImporterForWebOfLife.getNetworkNames(ResourceUtil.asInputStream(DatasetImporterForWebOfLife.WEB_OF_LIFE_BASE_URL + "/networkslist.php?type=All&data=All"));

        assertThat(networkNames, hasItem(URI.create("A_HP_002")));
        assertThat(networkNames.size() > 50, is(true));
    }


}

