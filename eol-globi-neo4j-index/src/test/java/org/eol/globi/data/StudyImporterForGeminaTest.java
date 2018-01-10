package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForGeminaTest extends GraphDBTestCase {

    private static final String firstFewLines = "ID\tPathogen\tPathogen Taxonomy\tSource\tTaxonomy ID\tTransmission Method\tHost/Reservoir\tTaxonomy ID\tDisease\tAnatomy\tSymptoms/Documentation/Attributes\n" +
            "TI:0001671\t71D1252 virus\t177895\tundocumented\t\tundocumented\tCulex\t7174\tno disease\tundocumented\tPMID:11581380,PMID:613619\n" +
            "TI:0001672\t78V3531 virus\t177898\tundocumented\t\tundocumented\tCulex\t7174\tno disease\tundocumented\tPMID:11581380\n" +
            "TI:0001893\tAmur virus\t86782\tundocumented\t\tundocumented\tHomo sapiens\t9606\tundocumented\tundocumented\tPMID: 17531126\n" +
            "TI:0000011\tBacillus anthracis\t1392\tair\t\tindirect: airborne\tEquus caballus\t9796\tAnthrax\trespiratory system\t\n" +
            "TI:0000010\tBacillus anthracis\t1392\tair\t\tindirect: airborne\tBos taurus\t9913\tAnthrax\trespiratory system\t\n" +
            "TI:0000009\tBacillus anthracis\t1392\tair\t\tindirect: airborne\tLoxodonta africana\t9785\tAnthrax\trespiratory system\t\n" +
            "TI:0000008\tBacillus anthracis\t1392\tair\t\tindirect: airborne\tSyncerus caffer caffer\t37445\tAnthrax\trespiratory system\t\n" +
            "TI:0000007\tBacillus anthracis\t1392\tair\t\tindirect: airborne\tHippopotamus amphibius\t9833\tAnthrax\trespiratory system\t\n" +
            "TI:0000006\tBacillus anthracis\t1392\tair\t\tindirect: airborne\tBubalus bubalis\t89462\tAnthrax\trespiratory system\t\n";

    @Test
    public void importFewLines() throws StudyImporterException {
        StudyImporterForGemina importer = new StudyImporterForGemina(new TestParserFactory(firstFewLines), nodeFactory);
        importStudy(importer);

        assertHuman();

        Taxon taxon = taxonIndex.findTaxonByName("Bacillus anthracis");
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getExternalId(), is("NCBI:1392"));

        List<String> antraxHosts = new ArrayList<String>();
        Iterable<Relationship> relationships = ((NodeBacked)taxon).getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.INCOMING);
        for (Relationship rel : relationships) {
            Node specimen = rel.getStartNode();
            Iterable<Relationship> pathogenRels = specimen.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.PATHOGEN_OF));
            for (Relationship pathogenRel : pathogenRels) {
                Relationship singleRelationship = pathogenRel.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
                antraxHosts.add(new TaxonNode(singleRelationship.getEndNode()).getName());
            }

        }

        assertThat(antraxHosts, hasItem("Equus caballus"));
    }

    private void assertHuman() throws NodeFactoryException {
        Taxon taxon = taxonIndex.findTaxonByName("Homo sapiens");
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getExternalId(), is("NCBI:9606"));
    }

}