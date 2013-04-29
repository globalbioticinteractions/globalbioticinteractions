package org.eol.globi.data;


import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Taxon;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class StudyImporterForGoMexSITest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(new ParserFactoryImpl(), nodeFactory);

        importer.importStudy();

        assertNotNull(nodeFactory.findTaxonOfType("Chloroscombrus chrysurus"));
        assertNotNull(nodeFactory.findTaxonOfType("Micropogonias undulatus"));

        assertNotNull(nodeFactory.findTaxonOfType("Amphipoda"));
        assertNotNull(nodeFactory.findTaxonOfType("Crustacea"));

        Taxon taxon = nodeFactory.findTaxonOfType("Scomberomorus cavalla");
        ArrayList<String> preyList = new ArrayList<String>();
        Iterable<Relationship> classifiedAsRels = taxon.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
        for (Relationship classifiedAsRel : classifiedAsRels) {
            Node specimen = classifiedAsRel.getStartNode();
            Specimen specimen1 = new Specimen(specimen);
            Iterable<Relationship> stomachContents = specimen1.getStomachContents();
            for (Relationship prey : stomachContents) {
                Relationship singleRelationship = prey.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
                preyList.add((String) singleRelationship.getEndNode().getProperty("name"));
            }
        }
        assertThat(preyList, hasItem("Organic matter"));
        assertThat(preyList, hasItem("Triglidae"));
        assertThat(preyList, hasItem("Sparidae"));

        assertNotNull(taxon);

        assertNotNull(nodeFactory.findLocation(29.346953, -92.980614, -13.641));

        assertNotNull(nodeFactory.findStudy(StudyImporterFactory.Study.GOMEXSI.toString()));
    }


}