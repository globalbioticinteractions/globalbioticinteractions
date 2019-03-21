package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.api.Neo4jTypes;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StudyImporterForBlewettTest extends GraphDBTestCase {

    static String dateToString(Date time) {
        DateTime dateTime = new DateTime(time);
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();
        return dateTimeFormatter.withZone(DateTimeZone.forID("US/Central")).print(dateTime);
    }

    @Override
    protected TermLookupService getTermLookupService() {
        return new UberonLookupService();
    }

    @Test
    public void parseDateTime() throws ParseException {
        Date date = StudyImporterForBlewett.parseDateString("1-Mar-00 10:55:00");
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        assertThat(instance.get(Calendar.YEAR), is(2000));
        assertThat(instance.get(Calendar.MONTH), is(Calendar.MARCH));
        assertThat(dateToString(date), is("2000-03-01T10:55:00.000-06:00"));
    }

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporter importer = new StudyImporterTestFactory(nodeFactory).instantiateImporter((Class) StudyImporterForBlewett.class);
        importStudy(importer);

        StudyNode study = getStudySingleton(getGraphDb());

        assertThat(getSpecimenCount(study), is(1824));

        assertNotNull(taxonIndex.findTaxonByName("Centropomus undecimalis"));
        Taxon taxonOfType = taxonIndex.findTaxonByName("Cal sapidus");
        assertThat(taxonOfType.getName(), is("Cal sapidus"));
        assertNotNull(taxonIndex.findTaxonByName("Ort chrysoptera"));
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
                "\"CHD01101502\",-82.1625,26.72,10:55:00,1-Mar-00,22.4,33.8\n" +
                "\"CHD01102504\",-82.1625,26.72,10:55:00,1-Mar-00,22.4,33.8\n" +
                "\"CHM000151\",-82.1625,26.72,10:55:00,1-Mar-00,22.4,33.8\n" +
                "\"CHM000152\",-82.103833,26.651833,12:40:00,1-Mar-00,24.8,30.3\n" +
                "\"CHM000153\",-82.087333,26.644833,13:40:00,1-Mar-00,25.1,30.1\n" +
                "\"CHM000154\",-82.083167,26.671167,14:40:00,1-Mar-00,26,30.4\n" +
                "\"CHM000175\",-82.197833,26.688167,10:00:00,8-Mar-00,22.2,35.05\n" +
                "\"CHM000176\",-82.191333,26.667333,11:00:00,8-Mar-00,22.7,35.25";


        final TestParserFactory preyPredatorFactory = new TestParserFactory(predatorPreyMapping);
        final TestParserFactory dateLocationFactory = new TestParserFactory(dateLocationString);


        ParserFactory testFactory = (studyResource, characterEncoding) -> {
            LabeledCSVParser parser;
            if (studyResource.contains("abundance")) {
                parser = preyPredatorFactory.createParser(studyResource, characterEncoding);
            } else {
                parser = dateLocationFactory.createParser(studyResource, characterEncoding);
            }
            return parser;
        };

        StudyImporter importer = new StudyImporterTestFactory(testFactory, nodeFactory).instantiateImporter(StudyImporterForBlewett.class);
        importStudy(importer);

        StudyNode study = getStudySingleton(getGraphDb());

        AtomicBoolean success = new AtomicBoolean(false);

        NodeUtil.RelationshipListener handler1 = collectedRel -> {
            Date unixEpochProperty = null;
            try {
                unixEpochProperty = nodeFactory.getUnixEpochProperty(new SpecimenNode(collectedRel.getEndNode()));
            } catch (NodeFactoryException e) {
                fail(e.getMessage());
            }
            assertThat(unixEpochProperty, is(not(nullValue())));
            String actual = dateToString(unixEpochProperty);
            Node predatorNode = collectedRel.getEndNode();
            if (StringUtils.equals(actual, "2000-03-01T10:55:00.000-06:00")
                    && predatorNode.hasProperty(SpecimenConstant.LIFE_STAGE_LABEL)
                    && predatorNode.hasRelationship(NodeUtil.asNeo4j(InteractType.ATE), Direction.OUTGOING)) {

                assertThat(predatorNode.getProperty(SpecimenConstant.LIFE_STAGE_LABEL), is("post-juvenile adult stage"));
                assertThat(predatorNode.getProperty(SpecimenConstant.LIFE_STAGE_ID), is("UBERON:0000113"));

                Node predatorTaxonNode = predatorNode.getRelationships(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).iterator().next().getEndNode();
                assertThat(predatorTaxonNode.getProperty(PropertyAndValueDictionary.NAME), is("Centropomus undecimalis"));

                Iterable<Relationship> ate = predatorNode.getRelationships(NodeUtil.asNeo4j(InteractType.ATE), Direction.OUTGOING);
                Node preyNode = ate.iterator().next().getEndNode();
                assertThat(preyNode, is(not(nullValue())));

                Node taxonNode = preyNode.getRelationships(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).iterator().next().getEndNode();
                assertThat(taxonNode, is(not(nullValue())));

                assertThat(taxonNode.getProperty(PropertyAndValueDictionary.NAME), is("Lag rhomboides"));
                success.set(true);
            }

        };

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), handler1, getGraphDb());
        assertTrue(success.get());

        success.set(false);

        NodeUtil.RelationshipListener handler2 = collectedRel -> {
            Node predatorNode = collectedRel.getEndNode();
            if (predatorNode.hasProperty(SpecimenConstant.LENGTH_IN_MM)
                    && (Double) predatorNode.getProperty(SpecimenConstant.LENGTH_IN_MM) == 548.0) {
                Iterable<Relationship> ate = predatorNode.getRelationships(NodeUtil.asNeo4j(InteractType.ATE), Direction.OUTGOING);
                assertThat(ate.iterator().hasNext(), is(false));

                Location location = null;
                try {
                    location = nodeFactory.findLocation(new LocationImpl(26.651833, -82.103833, 0.0, null));
                } catch (NodeFactoryException e) {
                    fail(e.getMessage());
                }
                assertThat(location, is(not(nullValue())));
                Iterable<Relationship> specimenCaughtHere = NodeUtil.getSpecimenCaughtHere(location);
                Iterator<Relationship> iterator = specimenCaughtHere.iterator();
                assertThat(iterator.hasNext(), is(true));
                iterator.next();
                assertThat(iterator.hasNext(), is(true));
                iterator.next();
                assertThat(iterator.hasNext(), is(true));
                iterator.next();
                assertThat(iterator.hasNext(), is(false));
                success.set(true);
            }

        };
        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), handler2, getGraphDb());
        assertTrue(success.get());

    }


}
