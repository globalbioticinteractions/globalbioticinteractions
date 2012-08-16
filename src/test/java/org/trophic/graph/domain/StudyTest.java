package org.trophic.graph.domain;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactory;
import org.trophic.graph.data.NodeFactoryException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.trophic.graph.domain.RelTypes.PART_OF;

public class StudyTest extends GraphDBTestCase {

    public static final String CARCHARODON = "Carcharodon";
    public static final String CARCHARODON_CARCHARIAS = CARCHARODON + " carcharias";
    public static final String CARASSIUS_AURATUS_AURATUS = "Carassius auratus auratus";
    public static final String WHITE_SHARK_FAMILY = "Lamnidae";
    public static final String NAME = "name";

    private NodeFactory factory;

    @Before
    public void createFactory() {
        factory = new NodeFactory(getGraphDb());
    }

    @Test
    public void populateStudy() throws NodeFactoryException {
        Study study = factory.createStudy("Our first study");


        Taxon family = factory.getOrCreateFamily(WHITE_SHARK_FAMILY);


        Taxon genus2 = factory.getOrCreateGenus(CARCHARODON);
        genus2.createRelationshipTo(family, PART_OF);
        Taxon genus = genus2;

        Taxon greatWhiteSpecies = factory.getOrCreateSpecies(genus, CARCHARODON_CARCHARIAS);


        Taxon goldFishSpecies = factory.getOrCreateSpecies(null, CARASSIUS_AURATUS_AURATUS);

        Specimen goldFish = factory.createSpecimen();
        goldFish.classifyAs(goldFishSpecies);

        Specimen shark = factory.createSpecimen();
        shark.classifyAs(greatWhiteSpecies);
        Specimen fuzzyShark = factory.createSpecimen();
        fuzzyShark.classifyAs(genus);

        shark.ate(goldFish);
        fuzzyShark.ate(goldFish);

        Location bolinasBay = factory.getOrCreateLocation(12.2d, 12.1d, -100.0d);
        shark.caughtIn(bolinasBay);

        Season winter = factory.createSeason("winter");
        shark.caughtDuring(winter);
        study.collected(shark);
        study.collected(fuzzyShark);

        shark.setLengthInMm(1.2d);

        Study foundStudy = this.factory.findStudy("Our first study");

        assertEquals(study.getTitle(), foundStudy.getTitle());

        for (Relationship rel : study.getSpecimens()) {
            Specimen specimen = new Specimen(rel.getEndNode());
            Relationship caughtDuringRel = rel.getEndNode().getSingleRelationship(RelTypes.CAUGHT_DURING, Direction.OUTGOING);
            if (caughtDuringRel != null) {
                Node seasonNode = caughtDuringRel.getEndNode();
                if (seasonNode != null && seasonNode.getProperty(Season.TITLE).equals("winter")) {
                    Relationship next = specimen.getClassifications().iterator().next();
                    Node endNode = next.getEndNode();
                    String speciesName = (String) endNode.getProperty("name");
                    assertEquals(CARCHARODON_CARCHARIAS, speciesName);
                    Node genusNode = endNode.getSingleRelationship(RelTypes.PART_OF, Direction.OUTGOING).getEndNode();
                    assertEquals(CARCHARODON, genusNode.getProperty("name"));
                    Node familyNode = genusNode.getSingleRelationship(RelTypes.PART_OF, Direction.OUTGOING).getEndNode();
                    assertEquals(WHITE_SHARK_FAMILY, familyNode.getProperty(NAME));
                    assertEquals(new Double(-100.0d), specimen.getSampleLocation().getAltitude());
                    assertEquals(new Double(1.2d), specimen.getLengthInMm());
                } else {
                    fail("expected to find a specimen");
                }
            } else if (specimen.equals(fuzzyShark)) {
                Node genusNode = specimen.getClassifications().iterator().next().getEndNode();
                assertEquals(CARCHARODON, genusNode.getProperty("name"));
            } else {
                fail("found unexpected specimen [" + specimen + "] in study");
            }
        }
    }

}
