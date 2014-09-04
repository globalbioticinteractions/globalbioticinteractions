package org.eol.globi.data;


import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForRobledoTest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException, NodeFactoryException {
        StudyImporterForRobledo importer = new StudyImporterForRobledo(new ParserFactoryImpl(), nodeFactory);

        Study study = importer.importStudy();

        assertNotNull(nodeFactory.findTaxonByName("Heliconia imbricata"));
        assertNotNull(nodeFactory.findTaxonByName("Renealmia alpinia"));

        assertNotNull(nodeFactory.findStudy(study.getTitle()));

        int count = 0;
        Iterable<Relationship> specimenRels = study.getSpecimens();
        for (Relationship specimenRel : specimenRels) {
            Specimen specimen1 = new Specimen(specimenRel.getEndNode());
            Location sampleLocation = specimen1.getSampleLocation();
            assertThat(sampleLocation, is(notNullValue()));
            assertThat(sampleLocation.getAltitude(), is(35.0));
            assertThat(Math.round(sampleLocation.getLongitude()), is(-84L));
            assertThat(Math.round(sampleLocation.getLatitude()), is(10L));
            count++;
        }

        assertThat(count, is(19));

    }


}