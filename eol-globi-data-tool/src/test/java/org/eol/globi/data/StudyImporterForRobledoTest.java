package org.eol.globi.data;


import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.Is.is;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import static org.junit.Assert.assertThat;
import static junit.framework.Assert.assertNotNull;

public class StudyImporterForRobledoTest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException, NodeFactoryException {
        StudyImporterForRobledo importer = new StudyImporterForRobledo(new ParserFactoryImpl(), nodeFactory);

        Study study = importer.importStudy();

        assertNotNull(nodeFactory.findTaxonOfType("Heliconia imbricata"));
        assertNotNull(nodeFactory.findTaxonOfType("Renealmia alpinia"));

        assertNotNull(nodeFactory.findStudy(StudyImporterFactory.Study.ROBLEDO.toString()));

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