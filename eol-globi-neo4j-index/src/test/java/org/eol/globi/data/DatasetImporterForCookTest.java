package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForCookTest extends GraphDBTestCase {

    @Test
    public void importFirstFewLines() throws IOException, NodeFactoryException, StudyImporterException {
        String firstFiveLines = "\"Date\",\"Individual\",\"Fish Length\",\"Infected\",\"Iso 1\",\"Iso 2\",\"Iso 3\",\"Iso 4 \",\"Iso 5\",\"Iso #\",,\n" +
                "8/10/2010,22,15.6,1,\"NA\",\"NA\",0,0,0,2,,\"Notes: All fish collected were Atlantic Croaker (Micropogonias undulatus) and measured in total length (cm). Isopods collected were Cymothoa excisa. For the Infected column 1= infected and 0= not infected. Numbers for the isopods are total length in cm and the Iso# represents the number of isopods found per fish. \"\n" +
                "8/10/2010,1,14.7,1,1.6,0.67,0,0,0,2,,\n" +
                "8/10/2010,5,14.2,1,1.53,0.7,0,0,0,2,,\n" +
                "8/10/2010,2,13.2,1,1.42,0.71,0.52,0.45,0,4,,\n";


        DatasetImporterForCook importer = new DatasetImporterForCook(new TestParserFactory(firstFiveLines), nodeFactory);
        importStudy(importer);
        StudyNode study = getStudySingleton(getGraphDb());

        Taxon hostTaxon = taxonIndex.findTaxonByName("Micropogonias undulatus");
        assertThat(hostTaxon, is(notNullValue()));
        Taxon parasiteTaxon = taxonIndex.findTaxonByName("Cymothoa excisa");
        assertThat(parasiteTaxon, is(notNullValue()));
        assertThat("missing location", nodeFactory.findLocation(new LocationImpl(27.85, -(97.0 + 8.0 / 60.0), -3.0, null)), is(notNullValue()));

        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean foundFirstHost = new AtomicBoolean(false);

        NodeUtil.RelationshipListener handler = relationship -> {
            assertThat(relationship.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH), is(notNullValue()));
            Node specimen = relationship.getEndNode();
            if (specimen.hasProperty(SpecimenConstant.LENGTH_IN_MM)) {
                Object property = specimen.getProperty(SpecimenConstant.LENGTH_IN_MM);
                if (new Double(156.0).equals(property)) {
                    assertTaxonClassification(specimen, ((NodeBacked) hostTaxon).getUnderlyingNode());
                    foundFirstHost.set(true);
                    Iterable<Relationship> parasiteRel = specimen.getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(InteractType.PARASITE_OF));
                    for (Relationship rel : parasiteRel) {
                        Node parasite = rel.getStartNode();
                        assertThat(parasite.hasProperty(SpecimenConstant.LENGTH_IN_MM), is(false));
                        assertTaxonClassification(parasite, ((NodeBacked) parasiteTaxon).getUnderlyingNode());
                    }
                }
            }
            count.incrementAndGet();

        };

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), handler);
        assertThat(count.get(), is(14));
        assertThat(foundFirstHost.get(), is(true));
    }

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporterForCook importer = new DatasetImporterForCook(new ParserFactoryLocal(), nodeFactory);
        importStudy(importer);
        StudyNode study = getStudySingleton(getGraphDb());

        assertThat(getSpecimenCount(study), is(1372));
    }


    private void assertTaxonClassification(Node parasite, Node underlyingNode) {
        Iterable<Relationship> classifiedAsRels = parasite.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
        for (Relationship classifiedAsRel : classifiedAsRels) {
            assertThat(classifiedAsRel.getEndNode(), is(underlyingNode));
        }
    }


}
