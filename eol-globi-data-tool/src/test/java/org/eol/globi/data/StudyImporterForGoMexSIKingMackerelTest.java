package org.eol.globi.data;


import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class StudyImporterForGoMexSIKingMackerelTest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGoMexSIKingMackerel importer = new StudyImporterForGoMexSIKingMackerel(new ParserFactoryImpl(), nodeFactory);

        importer.importStudy();

        Study study = nodeFactory.findStudy("Christmas et al 1974");

        assertSpecimenProperties();

        assertNotNull(study);
        assertThat(study.getTitle(), is("Christmas et al 1974"));
        assertThat(study.getContributor(), is("J Christmas, Allison Perry, Richard Waller"));
        assertThat(study.getPublicationYear(), is("1974"));
        assertThat(study.getInstitution(), is("NA"));
        assertThat(study.getDescription(), is("Investigations of coastal pelagic fishes"));

        assertNotNull(nodeFactory.findTaxonOfType("Chloroscombrus chrysurus"));
        assertNotNull(nodeFactory.findTaxonOfType("Micropogonias undulatus"));

        assertNotNull(nodeFactory.findTaxonOfType("Amphipoda"));
        assertNotNull(nodeFactory.findTaxonOfType("Crustacea"));

        Taxon taxon = nodeFactory.findTaxonOfType("Scomberomorus cavalla");
        List<String> preyList = new ArrayList<String>();
        final List<String> titles = new ArrayList<String>();
        Iterable<Relationship> classifiedAsRels = taxon.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
        int count = 0;
        for (Relationship classifiedAsRel : classifiedAsRels) {
            Node predatorSpecimen = classifiedAsRel.getStartNode();
            Specimen predator = new Specimen(predatorSpecimen);
            Iterable<Relationship> stomachContents = predator.getStomachContents();
            for (Relationship prey : stomachContents) {
                Relationship singleRelationship = prey.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
                preyList.add((String) singleRelationship.getEndNode().getProperty("name"));
            }
            count++;

            assertThat(predator.getSampleLocation(), is(notNullValue()));

            Relationship collectedBy = predatorSpecimen.getSingleRelationship(RelTypes.COLLECTED, Direction.INCOMING);
            assertThat(collectedBy, is(notNullValue()));
            String title = (String) collectedBy.getStartNode().getProperty("title");
            titles.add(title);
        }

        assertThat(count > 7, is(true));
        assertThat(preyList, hasItem("Organic matter"));
        assertThat(preyList, hasItem("Triglidae"));
        assertThat(preyList, hasItem("Sparidae"));

        assertThat(titles, hasItem("Beaumariage 1973"));
        assertThat(titles, hasItem("Blanton et al 1972"));

        assertNotNull(taxon);

        assertNotNull(nodeFactory.findLocation(29.346953, -92.980614, -13.641));

        assertNotNull(nodeFactory.findStudy("GoMexSI"));
    }

    private void assertSpecimenProperties() {
        Index<Node> taxa = getGraphDb().index().forNodes("taxons");
        boolean detectedAtLeastOneLifeState = false;
        boolean detectedAtLeastOnePhysiologicalState = false;
        boolean detectedAtLeastOnePreyBodyPart = false;

        assertThat(taxa, is(notNullValue()));

        for (Node taxonNode : taxa.query("name", "*")) {
            Iterable<Relationship> classifiedAs = taxonNode.getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
            for (Relationship classifiedA : classifiedAs) {
                Node specimenNode = classifiedA.getStartNode();
                detectedAtLeastOneLifeState |= specimenNode.hasProperty(Specimen.LIFE_STAGE);
                detectedAtLeastOnePhysiologicalState |= specimenNode.hasProperty(Specimen.PHYSIOLOGICAL_STATE);
                detectedAtLeastOnePreyBodyPart |= specimenNode.hasProperty(Specimen.BODY_PART);
            }
        }

        assertThat(detectedAtLeastOneLifeState, is(true));
        assertThat(detectedAtLeastOnePhysiologicalState, is(true));
        assertThat(detectedAtLeastOnePreyBodyPart, is(true));
    }


}