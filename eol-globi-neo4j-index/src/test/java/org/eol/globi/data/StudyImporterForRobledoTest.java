package org.eol.globi.data;


import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForRobledoTest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException, NodeFactoryException {
        StudyImporterForRobledo importer = new StudyImporterForRobledo(new ParserFactoryLocal(), nodeFactory);

        importStudy(importer);
        StudyNode study = getStudySingleton(getGraphDb());

        assertNotNull(taxonIndex.findTaxonByName("Heliconia imbricata"));
        assertNotNull(taxonIndex.findTaxonByName("Renealmia alpinia"));

        assertNotNull(nodeFactory.findStudy(study.getTitle()));

        AtomicInteger count = new AtomicInteger(0);
        Iterable<Relationship> specimenRels = NodeUtil.getSpecimens(study);
        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), new NodeUtil.RelationshipListener() {
            @Override
            public void on(Relationship relationship) {
                Specimen specimen1 = new SpecimenNode(relationship.getEndNode());
                Location sampleLocation = specimen1.getSampleLocation();
                assertThat(sampleLocation, is(notNullValue()));
                assertThat(sampleLocation.getAltitude(), is(35.0));
                assertThat(Math.round(sampleLocation.getLongitude()), is(-84L));
                assertThat(Math.round(sampleLocation.getLatitude()), is(10L));
                count.incrementAndGet();

            }
        }, getGraphDb());

        assertThat(count.get(), is(93));
    }


}