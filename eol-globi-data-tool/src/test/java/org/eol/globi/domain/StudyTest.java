package org.eol.globi.domain;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StudyTest extends GraphDBTestCase {

    public static final String CARCHARODON = "Carcharodon";
    public static final String CARCHARODON_CARCHARIAS = CARCHARODON + " carcharias";
    public static final String CARASSIUS_AURATUS_AURATUS = "Carassius auratus auratus";

    @Test
    public void populateStudy() throws NodeFactoryException {
        StudyNode study = nodeFactory.createStudy("Our first study");

        taxonIndex.getOrCreateTaxon(CARCHARODON_CARCHARIAS);

        SpecimenNode goldFish = nodeFactory.createSpecimen(study, CARASSIUS_AURATUS_AURATUS);

        Specimen shark = nodeFactory.createSpecimen(study, CARCHARODON_CARCHARIAS);
        Specimen fuzzyShark = nodeFactory.createSpecimen(study, CARCHARODON);

        shark.ate(goldFish);
        fuzzyShark.ate(goldFish);

        Location bolinasBay = nodeFactory.getOrCreateLocation(12.2d, 12.1d, -100.0d);
        shark.caughtIn(bolinasBay);

        SeasonNode winter = nodeFactory.createSeason("winter");
        shark.caughtDuring(winter);

        shark.setLengthInMm(1.2d);
        resolveNames();

        Study foundStudy = this.nodeFactory.findStudy("Our first study");

        assertEquals(study.getTitle(), foundStudy.getTitle());

        for (Relationship rel : NodeUtil.getSpecimens(study)) {
            Specimen specimen = new SpecimenNode(rel.getEndNode());
            Relationship caughtDuringRel = rel.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CAUGHT_DURING), Direction.OUTGOING);
            if (caughtDuringRel != null) {
                Node seasonNode = caughtDuringRel.getEndNode();
                if (seasonNode != null && seasonNode.getProperty(SeasonNode.TITLE).equals("winter")) {
                    Relationship next = NodeUtil.getClassifications(specimen).iterator().next();
                    Node endNode = next.getEndNode();
                    String speciesName = (String) endNode.getProperty("name");
                    assertEquals(CARCHARODON_CARCHARIAS, speciesName);
                    assertEquals(new Double(-100.0d), specimen.getSampleLocation().getAltitude());
                    assertEquals(new Double(1.2d), specimen.getLengthInMm());
                } else {
                    fail("expected to findNamespaces a specimen");
                }
            } else if (specimen.equals(goldFish)) {
                Node genusNode = NodeUtil.getClassifications(specimen).iterator().next().getEndNode();
                assertEquals(CARASSIUS_AURATUS_AURATUS, genusNode.getProperty("name"));
            } else if (specimen.equals(fuzzyShark)) {
                Node genusNode = NodeUtil.getClassifications(specimen).iterator().next().getEndNode();
                assertEquals(CARCHARODON, genusNode.getProperty("name"));
            } else {
                fail("found unexpected specimen [" + specimen + "] in study");
            }
        }
    }

}
