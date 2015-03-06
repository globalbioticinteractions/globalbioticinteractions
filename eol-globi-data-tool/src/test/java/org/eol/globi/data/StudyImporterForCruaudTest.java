package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForCruaudTest extends GraphDBTestCase {

    @Test
    public void importAll() throws NodeFactoryException, StudyImporterException {
        StudyImporterForCruaud importer = new StudyImporterForCruaud(new ParserFactoryImpl(), nodeFactory);
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
        importer.importStudy();

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));


        assertThat(nodeFactory.findTaxonByName("Agaon sp."), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Ficus chapaensis"), is(notNullValue()));

        assertThat(allStudies.get(0).getSource(), is(notNullValue()));
    }
}
