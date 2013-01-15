package org.trophic.graph.data;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.domain.Study;

import java.util.Iterator;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StudyImporterForJRFerrerParisTest extends GraphDBTestCase {

    @Test
    public void testFullImport() throws StudyImporterException {
        StudyImporterForJRFerrerParis studyImporterForJRFerrerParis = new StudyImporterForJRFerrerParis(new ParserFactoryImpl(), nodeFactory);
        Study study = studyImporterForJRFerrerParis.importStudy();
        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }
        assertTrue(count > 0);

    }

    @Test
    public void testSomeLine() throws StudyImporterException, NodeFactoryException {
        String csvContent = "\"\",\"Lepidoptera Family\",\"Lepidoptera Name\",\"Hostplant Family\",\"Hostplant Name\",\"Country\",\"reference\"\n" +
                "\"27385\",\"Pieridae\",\"Hesperocharis anguitia\",\"Santalales\",\"'Loranthus'\",\"Brazil\",\"Braby & Nishida 2007\"\n" +
                "\"27386\",\"Pieridae\",\"Mathania carrizoi\",\"Santalales\",\"'Loranthus'\",\"Argentina\",\"Braby & Nishida 2007\"\n" +
                "\"27387\",\"Pieridae\",\"Mylothris agathina\",\"Santalales\",\"?Loranthus? spp.\",\"Kenya, Tanzania, South Africa\",\"Braby 2005\"\n" +
                "\"27388\",\"Pieridae\",\"Mylothris chloris\",\"Santalales\",\"?Loranthus? spp.\",\"Kenya\",\"Braby 2005\"";

        StudyImporterForJRFerrerParis studyImporterFor = new StudyImporterForJRFerrerParis(new TestParserFactory(csvContent), nodeFactory);

        Study study = studyImporterFor.importStudy();
        assertNotNull(nodeFactory.findTaxonOfType("Hesperocharis anguitia"));
        assertNotNull(nodeFactory.findTaxonOfType("?Loranthus? spp.?"));

        Iterable<Relationship> collectedRels = study.getSpecimens();
        int totalRels = 0;
        for (Relationship collectedRel : collectedRels) {
            Node endNode = collectedRel.getEndNode();
            Iterable<Relationship> relationships = endNode.getRelationships(Direction.OUTGOING, RelTypes.ATE);
            assertThat(relationships.iterator().hasNext(), Is.is(true));
            Node targetSpecimen = relationships.iterator().next().getEndNode();
            Iterator<Relationship> targetClassificationRel = targetSpecimen.getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS).iterator();
            assertThat(targetClassificationRel.hasNext(), Is.is(true));
            Node targetTaxon = targetClassificationRel.next().getEndNode();
            assertThat(targetTaxon, Is.is(notNullValue()));
            totalRels++;
        }

        assertThat(totalRels, Is.is(4));
    }
}
