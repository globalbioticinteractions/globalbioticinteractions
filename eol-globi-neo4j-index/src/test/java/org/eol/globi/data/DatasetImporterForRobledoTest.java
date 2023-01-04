package org.eol.globi.data;


import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForRobledoTest extends GraphDBNeo4j2TestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException {
        DatasetImporterForRobledo importer = new DatasetImporterForRobledo(
                new ParserFactoryLocal(getClass()), nodeFactory
        );

        importStudy(importer);
        StudyNode study = getStudySingleton(getGraphDb());

        assertNotNull(taxonIndex.findTaxonByName("Heliconia imbricata"));
        assertNotNull(taxonIndex.findTaxonByName("Renealmia alpinia"));

        assertNotNull(nodeFactory.findStudy(study));

        AtomicInteger count = new AtomicInteger(0);
        NodeUtil.handleCollectedRelationships(
                new NodeTypeDirection(study.getUnderlyingNode())
                , relationship -> {
                    Specimen specimen1 = new SpecimenNode(relationship.getEndNode());
                    Location sampleLocation = specimen1.getSampleLocation();
                    assertThat(sampleLocation, is(notNullValue()));
                    assertThat(sampleLocation.getAltitude(), is(35.0));
                    assertThat(Math.round(sampleLocation.getLongitude()), is(-84L));
                    assertThat(Math.round(sampleLocation.getLatitude()), is(10L));
                    count.incrementAndGet();

                });

        assertThat(count.get(), is(93));
    }


}