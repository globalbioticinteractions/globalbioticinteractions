package org.trophic.graph.data;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.domain.Season;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForLavacaBayTest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudyFromLavacaBay() throws StudyImporterException, NodeFactoryException {
        String csvString =
                "\"Region\",\"Season\",\"Habitat\",\"Site\",\"Family\",\"Predator Species\",\"TL\",\"Prey Item Species\",\"Prey item\",\"Number\",\"Condition Index\",\"Volume\",\"Percent Content\",\"Prey Item Trophic Level\",\"Notes\"\n";
        csvString += "\"Lower\",\"Fall\",\"Marsh\",1,\"Sciaenidae\",\"Sciaenops ocellatus\",420,\"Acrididae spp. \",\"Acrididae \",1,\"III\",0.4,3.2520325203,2.5,\n";
        csvString += "\"Lower\",\"Spring\",\"Non-Veg \",1,\"Ariidae\",\"Arius felis\",176,\"Aegathoa oculata \",\"Aegathoa oculata\",4,\"I\",0.01,3.3333333333,2.1,\n";
        StudyImporterForLavacaBay studyImporterFor = new StudyImporterForLavacaBay(new TestParserFactory(csvString), nodeFactory, StudyLibrary.Study.LACAVA_BAY);


        Study study = studyImporterFor.importStudy();

        assertNotNull(nodeFactory.findTaxonOfType("Sciaenidae", Taxon.FAMILY));
        assertNotNull(nodeFactory.findTaxonOfType("Ariidae", Taxon.FAMILY));
        assertNotNull(nodeFactory.findTaxonOfType("Sciaenops ocellatus", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Sciaenops", Taxon.GENUS));
        assertNotNull(nodeFactory.findTaxonOfType("Arius felis", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Arius", Taxon.GENUS));

        assertNotNull(nodeFactory.findTaxonOfType("Acrididae", Taxon.FAMILY));
        assertNotNull(nodeFactory.findTaxonOfType("Arius", Taxon.GENUS));

        assertNotNull(nodeFactory.findTaxonOfType("Aegathoa oculata", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Aegathoa", Taxon.GENUS));

        assertNotNull(nodeFactory.findStudy(StudyImporterForMississippiAlabama.LAVACA_BAY_DATA_SOURCE));

        assertNotNull(nodeFactory.findSeason("spring"));
        assertNotNull(nodeFactory.findSeason("fall"));

        Study foundStudy = nodeFactory.findStudy(StudyImporterForMississippiAlabama.LAVACA_BAY_DATA_SOURCE);
        assertNotNull(foundStudy);
        for (Relationship rel : study.getSpecimens()) {
            Specimen specimen = new Specimen(rel.getEndNode());
            for (Relationship ateRel : specimen.getStomachContents()) {
                Taxon taxon = new Taxon(rel.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode());
                String scientificName = taxon.getName();
                if ("Sciaenops ocellatus".equals(scientificName)) {
                    Taxon genus = new Taxon(taxon.isA());
                    assertEquals("Sciaenops", genus.getName());
                    assertEquals("Sciaenidae", new Taxon(genus.isA()).getName());
                    Location sampleLocation = specimen.getSampleLocation();
                    assertNull(sampleLocation);
                    Iterable<Relationship> stomachContents = specimen.getStomachContents();
                    int count = 0;
                    for (Relationship containsRel : stomachContents) {
                        Node endNode = containsRel.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode();
                        Object name = endNode.getProperty("name");
                        assertEquals("Acrididae", name);
                        count++;
                    }
                    assertEquals(1, count);
                    Season season = specimen.getSeason();
                    assertEquals("fall", season.getTitle());
                    assertEquals(420.0d, specimen.getLengthInMm());
                    Location location = specimen.getSampleLocation();
//                    assertThat(location, is(not(nullValue())));
//                    assertThat(location.getLatitude(), is(Math.abs(28.595267 + 28.596233)/2.0));
//                    assertThat(location.getLongitude(), is(Math.abs(-96.477033 - 96.476483)/2.0));

                } else if ("Arius felis".equals(scientificName)) {
                    Location sampleLocation = specimen.getSampleLocation();
                    assertNull(sampleLocation);

                    Iterable<Relationship> stomachContents = specimen.getStomachContents();
                    int count = 0;
                    for (Relationship containsRel : stomachContents) {
                        Object name = containsRel.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode().getProperty("name");
                        assertEquals("Aegathoa oculata", name);
                        count++;
                    }
                    assertEquals(1, count);

                    Season season = specimen.getSeason();
                    assertEquals("spring", season.getTitle());

                    assertEquals(176.0d, specimen.getLengthInMm());

                    Location location = specimen.getSampleLocation();
//                    assertThat(location, is(not(nullValue())));
//                    assertThat(location.getLatitude(), is(Math.abs(28.608417 + 28.607217)/2.0));
//                    assertThat(location.getLongitude(), is(Math.abs(-96.475517 - 96.474500)/2.0));
                } else {
                    fail("unexpected scientificName of predator [" + scientificName + "]");
                }

            }

        }
    }
}
