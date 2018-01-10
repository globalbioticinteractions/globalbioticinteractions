package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForBascompteIT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, IOException {
        StudyImporterForWebOfLife importer = new StudyImporterForWebOfLife(null, nodeFactory);
        importStudy(importer);

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        List<String> references = new ArrayList<String>();
        Set<String> referenceSet = new HashSet<String>();
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("Web of Life. Accessed at http://www.web-of-life.es/"));
            references.add(allStudy.getCitation());
            referenceSet.add(allStudy.getCitation());
        }

        assertThat(references.size(), is(referenceSet.size()));
        assertThat(references, hasItem("Arroyo, M.T.K., R. Primack & J.J. Armesto. 1982. Community studies in pollination ecology in the high temperate Andes of central Chile. I. Pollination mechanisms and altitudinal variation. Amer. J. Bot. 69:82-97."));
        assertThat(taxonIndex.findTaxonByName("Diplopterys pubipetala"), is(notNullValue()));
    }

    @Test
    public void getNetworkNames() throws IOException {
        final List<String> networkNames = StudyImporterForWebOfLife.getNetworkNames(ResourceUtil.asInputStream(StudyImporterForWebOfLife.WEB_OF_LIFE_BASE_URL + "/networkslist.php?type=All&data=All"));
        assertThat(networkNames, hasItem("A_HP_001"));
    }
}
