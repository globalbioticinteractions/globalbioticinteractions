package org.eol.globi.data;

import org.eol.globi.domain.StudyNode;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForCruaudTest extends GraphDBNeo4j2TestCase {

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporterForCruaud importer = new DatasetImporterForCruaud(new ParserFactoryLocal(getClass()), nodeFactory);
        importer.setGeoNamesService(new GeoNamesService() {
            @Override
            public boolean hasTermForLocale(String locality) {
                return true;
            }

            @Override
            public LatLng findLatLng(String locality) throws IOException {
                return new LatLng(0,0);
            }
        });
        importStudy(importer);

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));


        assertThat(taxonIndex.findTaxonByName("Agaon sp."), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Ficus chapaensis"), is(notNullValue()));

        assertThat(allStudies.get(0).getCitation(), is(notNullValue()));
    }
}
