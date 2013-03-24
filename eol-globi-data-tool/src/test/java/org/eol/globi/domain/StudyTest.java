package org.eol.globi.domain;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.taxon.TaxonLookupService;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.eol.globi.domain.RelTypes.IS_A;

public class StudyTest extends GraphDBTestCase {

    public static final String CARCHARODON = "Carcharodon";
    public static final String CARCHARODON_CARCHARIAS = CARCHARODON + " carcharias";
    public static final String CARASSIUS_AURATUS_AURATUS = "Carassius auratus auratus";
    public static final String WHITE_SHARK_FAMILY = "Lamnidae";

    private NodeFactory factory;

    @Before
    public void createFactory() {
        factory = new NodeFactory(getGraphDb(), new TaxonLookupService() {
            @Override
            public String[] lookupTermIds(String taxonName) throws IOException {
                return new String[0];
            }

            @Override
            public void destroy() {

            }
        });
    }

    @Test
    public void populateStudy() throws NodeFactoryException {
        Study study = factory.createStudy("Our first study");


        Taxon family = factory.getOrCreateTaxon(WHITE_SHARK_FAMILY);


        Taxon genus2 = factory.getOrCreateTaxon(CARCHARODON);
        genus2.createRelationshipTo(family, IS_A);
        Taxon genus = genus2;

        Taxon greatWhiteSpecies = factory.getOrCreateTaxon(CARCHARODON_CARCHARIAS);


        Taxon goldFishSpecies = factory.getOrCreateTaxon(CARASSIUS_AURATUS_AURATUS);

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
