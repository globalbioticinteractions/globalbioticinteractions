package org.trophic.graph.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class StudyImporterForSnookTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporter importer = new StudyImporterFactory(new ParserFactoryImpl(), nodeFactory).createImporterForStudy(StudyLibrary.Study.BLEWETT_CHARLOTTE_HARBOR_FL);
        importer.importStudy();

        assertNotNull(nodeFactory.findTaxonOfType("Centropomus undecimalis", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Cal sapidus", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Ort chrysoptera", Taxon.SPECIES));
    }

    @Test
    public void importLines() throws StudyImporterException {
        String predatorPreyMapping = "\"Collection #\",\"Sp#\",\"Standard Length\",\"ID\",\"Far duoraum\",\"Cal sapidus\",\"Unid fish\",\"Anchoa spp\",\"Mug gyrans\",\"Bai chrysoura\",\"Portunus spp\",\"Bivalves\",\"Portunidae\",\"Lag rhomboides\",\"Xanthidae\",\"Palaemonidae\",\"Eucinostomus spp\",\"Mugil spp\",\"Alpheidae\",\"Atherinidae\",\"Syn foetens\",\"Ort chrysoptera\",\"Snails\",\"Euc gula\",\"Cynoscion spp\",\"Cyp. Variegatus\",\"Fun majalis\",\"Poe latipinna\",\"Unid crab\",\"Har jaguana\",\"Arm mierii\",\"Fun grandis\",\"Mic gulosus\",\"Ari felis\",\"Clupeidae\",\"Fundulus spp\",\"Diapterus/Eugerres spp\",\"Isopods\",\"Cyn nebulosus\",\"Opi oglinum\",\"Flo carpio\",\"Luc parva\",\"Uca spp\",\"Majidae\",\"Mug cephalus\",\"Squ empusa\",\"Opi robinsi\",\"Ariidae\",\"Sci ocellatus\",\"Unid shrimp\",\"Uca thayeri\",\"Grapsidae\",\"Lei xanthurus\",\"Elo saurus\",\"Brevoortia spp\"\n" +
                "\"CHD01101502\",1,549,,,,,,,,,,,1,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "\"CHD01102504\",1,548,\"E\",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "\"CHD01102504\",2,550,,3,,,,,,,,,1,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "\"CHM000152\",1,580,\"E\",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "\"CHM000152\",2,556,,,1,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";

        String dateLocationString = "\"Collection #\",\"Longitude\",\"Latitude\",\"Time\",\"Date\",\"Temperature\",\"Salinity\"\n" +
                "\"CHM000151\",-82.1625,26.72,10:55:00,1-Mar-00,22.4,33.8\n" +
                "\"CHM000152\",-82.103833,26.651833,12:40:00,1-Mar-00,24.8,30.3\n" +
                "\"CHM000153\",-82.087333,26.644833,13:40:00,1-Mar-00,25.1,30.1\n" +
                "\"CHM000154\",-82.083167,26.671167,14:40:00,1-Mar-00,26,30.4\n" +
                "\"CHM000175\",-82.197833,26.688167,10:00:00,8-Mar-00,22.2,35.05\n" +
                "\"CHM000176\",-82.191333,26.667333,11:00:00,8-Mar-00,22.7,35.25";


        final TestParserFactory preyPredatorFactory = new TestParserFactory(predatorPreyMapping);
        final TestParserFactory dateLocationFactory = new TestParserFactory(dateLocationString);


        ParserFactory testFactory = new ParserFactory() {
            @Override
            public LabeledCSVParser createParser(String studyResource) throws IOException {
                LabeledCSVParser parser = null;
                if (studyResource.contains("abundance")) {
                    parser = preyPredatorFactory.createParser(studyResource);
                } else {
                    parser = dateLocationFactory.createParser(studyResource);
                }
                return parser;
            }
        };

        StudyImporter importer = new StudyImporterFactory(testFactory, nodeFactory).createImporterForStudy(StudyLibrary.Study.BLEWETT_CHARLOTTE_HARBOR_FL);
        Study study = importer.importStudy();
        assertThat(study.getTitle(), is("Blewett2000CharlotteHarborFL"));

        assertNotNull(study);

        Iterable<Relationship> specimens = study.getSpecimens();
        Relationship next = specimens.iterator().next();
        Node predatorNode = next.getEndNode();
        assertThat((Double) predatorNode.getProperty(Specimen.LENGTH_IN_MM), is(549.0));

        Node predatorTaxonNode = predatorNode.getRelationships(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).iterator().next().getEndNode();
        assertThat((String) predatorTaxonNode.getProperty(Taxon.NAME), is("Centropomus undecimalis"));

        Iterable<Relationship> ate = predatorNode.getRelationships(RelTypes.ATE, Direction.OUTGOING);
        Node preyNode = ate.iterator().next().getEndNode();
        assertThat(preyNode, is(not(nullValue())));

        Node taxonNode = preyNode.getRelationships(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).iterator().next().getEndNode();
        assertThat(taxonNode, is(not(nullValue())));

        assertThat((String) taxonNode.getProperty(Taxon.NAME), is("Lag rhomboides"));

        next = specimens.iterator().next();
        predatorNode = next.getEndNode();
        assertThat((Double) predatorNode.getProperty(Specimen.LENGTH_IN_MM), is(548.0));

        ate = predatorNode.getRelationships(RelTypes.ATE, Direction.OUTGOING);
        assertThat(ate.iterator().hasNext(), is(false));

        Location location = nodeFactory.findLocation(26.651833, -82.103833, 0.0);
        assertThat(location, is(not(nullValue())));
        Iterable<Relationship> specimenCaughtHere = location.getSpecimenCaughtHere();
        Iterator<Relationship> iterator = specimenCaughtHere.iterator();
        assertThat(iterator.hasNext(), is(true));
        iterator.next();
        assertThat(iterator.hasNext(), is(true));
        iterator.next();
        assertThat(iterator.hasNext(), is(false));
    }


}
