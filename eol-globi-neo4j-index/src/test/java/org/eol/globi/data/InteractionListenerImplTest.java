package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.tool.StudyImportLogger;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.MapUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static org.eol.globi.data.StudyImporterForTSV.ARGUMENT_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.DECIMAL_LATITUDE;
import static org.eol.globi.data.StudyImporterForTSV.DECIMAL_LONGITUDE;
import static org.eol.globi.data.StudyImporterForTSV.LOCALITY_ID;
import static org.eol.globi.data.StudyImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_BODY_PART_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_BODY_PART_NAME;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_LIFE_STAGE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_NAME;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_NAME;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InteractionListenerImplTest extends GraphDBTestCase {

    @Test
    public void importBlankCitation() throws StudyImporterException {
        final InteractionListenerImpl listener = new InteractionListenerImpl(nodeFactory, null, null);
        final HashMap<String, String> link = new HashMap<String, String>();
        link.put(SOURCE_OCCURRENCE_ID, "123");
        link.put(TARGET_OCCURRENCE_ID, "456");
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_TAXON_ID, "duck");
        link.put(SOURCE_BODY_PART_ID, "bla:123");
        link.put(SOURCE_BODY_PART_NAME, "snout");
        link.put(SOURCE_LIFE_STAGE_ID, "some:stage");
        link.put(SOURCE_LIFE_STAGE_NAME, "stage");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(TARGET_TAXON_ID, "mouse");
        link.put(TARGET_BODY_PART_ID, "bla:345");
        link.put(TARGET_BODY_PART_NAME, "tail");
        link.put(StudyImporterForMetaTable.EVENT_DATE, "20160404T21:31:40Z");
        link.put(StudyImporterForMetaTable.LATITUDE, "12.1");
        link.put(StudyImporterForMetaTable.LONGITUDE, "13.2");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "doi:1234");
        listener.newLink(link);

        final AtomicBoolean foundPair = new AtomicBoolean(false);
        NodeUtil.RelationshipListener relationshipListener = relationship -> {
            final SpecimenNode predator = new SpecimenNode(relationship.getEndNode());
            for (Relationship stomachRel : NodeUtil.getStomachContents(predator)) {
                final SpecimenNode prey = new SpecimenNode(stomachRel.getEndNode());
                final TaxonNode preyTaxon = getOrigTaxon(prey);
                final TaxonNode predTaxon = getOrigTaxon(predator);
                assertThat(preyTaxon.getName(), is("mini"));
                assertThat(preyTaxon.getExternalId(), is("mouse"));
                assertThat(predTaxon.getName(), is("donald"));
                assertThat(predTaxon.getExternalId(), is("duck"));

                assertLocation(predator.getSampleLocation());
                assertLocation(prey.getSampleLocation());

                assertThat(predator.getExternalId(), is("123"));
                assertThat(prey.getExternalId(), is("456"));

                assertThat(predator.getLifeStage().getId(), is("some:stage"));
                assertThat(predator.getLifeStage().getName(), is("stage"));
                foundPair.set(true);

                assertThat(relationship.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH), is(notNullValue()));

                List<SpecimenNode> specimens = Arrays.asList(predator, prey);
                for (SpecimenNode specimen : specimens) {
                    assertThat(specimen.getBodyPart().getId(), is(notNullValue()));
                    assertThat(specimen.getBodyPart().getName(), is(notNullValue()));
                }
            }

        };
        handleRelations(relationshipListener, RelTypes.COLLECTED);
        assertThat(foundPair.get(), is(true));
    }

    private void handleRelations(NodeUtil.RelationshipListener handler, RelTypes collected) {
        final List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        final StudyNode study = (StudyNode) allStudies.get(0);
        assertThat(study.getCitation(), is(""));
        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode(), collected), handler, 1);
    }

    @Test
    public void importAssociatedTaxa() throws StudyImporterException {
        final InteractionListenerImpl listener = getAssertingListener();
        final HashMap<String, String> link = new HashMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_TAXON_ID, "duck");
        link.put(SOURCE_BODY_PART_ID, "bla:123");
        link.put(SOURCE_BODY_PART_NAME, "snout");
        link.put("associatedTaxa", "parasite of: Mini mouse");
        link.put(StudyImporterForMetaTable.EVENT_DATE, "20160404T21:31:40Z");
        link.put(StudyImporterForMetaTable.LATITUDE, "12.1");
        link.put(StudyImporterForMetaTable.LONGITUDE, "13.2");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "doi:1234");
        listener.newLink(link);

        final AtomicBoolean foundPair = new AtomicBoolean(false);
        NodeUtil.RelationshipListener relationshipListener = relationship -> {
            final SpecimenNode predator = new SpecimenNode(relationship.getEndNode());
            for (Relationship hosts : predator.getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(InteractType.PARASITE_OF), Direction.OUTGOING)) {
                final SpecimenNode host = new SpecimenNode(hosts.getEndNode());
                final TaxonNode hostTaxon = getOrigTaxon(host);
                final TaxonNode predTaxon = getOrigTaxon(predator);
                assertThat(hostTaxon.getName(), is("Mini mouse"));
                assertThat(predTaxon.getName(), is("donald"));
                assertThat(predTaxon.getExternalId(), is("duck"));

                foundPair.set(true);
            }

        };

        handleRelations(relationshipListener, RelTypes.COLLECTED);
        assertThat(foundPair.get(), is(true));
    }

    public InteractionListenerImpl getAssertingListener() {
        return new InteractionListenerImpl(nodeFactory, null, new ImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                fail("got message: " + message);
            }

            @Override
            public void info(LogContext ctx, String message) {

            }

            @Override
            public void severe(LogContext ctx, String message) {
                fail("got message: " + message);
            }
        });
    }

    @Test
    public void importRefutingClaim() throws StudyImporterException {
        final InteractionListenerImpl listener = getAssertingListener();
        final HashMap<String, String> link = new HashMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_TAXON_ID, "duck");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(TARGET_TAXON_ID, "mouse");
        link.put(ARGUMENT_TYPE_ID, "https://en.wiktionary.org/wiki/refute");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "doi:1234");
        listener.newLink(link);

        AtomicBoolean foundSpecimen = new AtomicBoolean(false);
        NodeUtil.RelationshipListener relHandler = relationship -> {
            final SpecimenNode someSpecimen = new SpecimenNode(relationship.getEndNode());
            assertTrue(someSpecimen.getUnderlyingNode().hasRelationship(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.REFUTES)));
            assertFalse(someSpecimen.getUnderlyingNode().hasRelationship(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.COLLECTED)));
            foundSpecimen.set(true);
        };
        handleRelations(relHandler, RelTypes.REFUTES);

        assertThat(foundSpecimen.get(), is(true));
    }

    @Test
    public void importWithLocalityAndLatLng() throws StudyImporterException {
        final InteractionListenerImpl listener = getAssertingListener();
        final HashMap<String, String> link = new HashMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_TAXON_ID, "duck");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(TARGET_TAXON_ID, "mouse");
        link.put(LOCALITY_ID, "back:yard");
        link.put(LOCALITY_NAME, "my back yard");
        link.put(DECIMAL_LATITUDE, "12.2");
        link.put(DECIMAL_LONGITUDE, "13.2");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "doi:1234");
        listener.newLink(link);

        AtomicBoolean foundSpecimen = new AtomicBoolean(false);
        NodeUtil.RelationshipListener someListener = relationship -> {
            final SpecimenNode someSpecimen = new SpecimenNode(relationship.getEndNode());
            assertTrue(someSpecimen.getUnderlyingNode().hasRelationship(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.COLLECTED)));
            LocationNode sampleLocation = someSpecimen.getSampleLocation();
            assertThat(sampleLocation.getLatitude(), is(12.2d));
            assertThat(sampleLocation.getLongitude(), is(13.2d));
            assertThat(sampleLocation.getLocality(), is("my back yard"));
            assertThat(sampleLocation.getLocalityId(), is("back:yard"));
            foundSpecimen.set(true);
        };

        handleRelations(someListener, RelTypes.COLLECTED);

        assertThat(foundSpecimen.get(), is(true));
    }

    @Test
    public void importWithSymbiotaDateTimeYearOnly() throws StudyImporterException {
        assertSymbiotaDateString("2016-00-00", "2016-01-01T00:00:00Z");
    }

    @Test
    public void importWithSymbiotaDateTimeMonth() throws StudyImporterException {
        assertSymbiotaDateString("2016-04-00", "2016-04-01T00:00:00Z");
    }

    @Test
    public void importWithSymbiotaDateTimeDayNotMonth() throws StudyImporterException {
        assertSymbiotaDateString("2016-00-30", "2016-01-01T00:00:00Z");
    }

    @Test
    public void importWithDateTimezone() throws StudyImporterException {
        assertSymbiotaDateString("2019-09-23T20:34:37-00:00", "2019-09-23T20:34:37Z");
    }

    public void assertSymbiotaDateString(String symbiotaTime, String expectedUTC) throws StudyImporterException {
        final InteractionListenerImpl listener = getAssertingListener();
        final HashMap<String, String> link = new HashMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(StudyImporterForMetaTable.EVENT_DATE, symbiotaTime);
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        listener.newLink(link);

        AtomicBoolean foundSpecimen = new AtomicBoolean(false);
        NodeUtil.RelationshipListener someListener = relationship -> {

            final SpecimenNode someSpecimen = new SpecimenNode(relationship.getEndNode());
            assertTrue(someSpecimen.getUnderlyingNode().hasRelationship(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.COLLECTED)));
            try {
                Date eventDate = nodeFactory.getUnixEpochProperty(someSpecimen);
                assertThat(DateUtil.printDate(eventDate), is(expectedUTC));
            } catch (NodeFactoryException e) {
                fail(e.getMessage());
            }
            foundSpecimen.set(true);
        };

        handleRelations(someListener, RelTypes.COLLECTED);

        assertThat(foundSpecimen.get(), is(true));
    }

    public void assertLocation(LocationNode sampleLocation) {
        assertThat(sampleLocation.getLatitude(), is(12.1d));
        assertThat(sampleLocation.getLongitude(), is(13.2d));
    }

    public TaxonNode getOrigTaxon(SpecimenNode predator) {
        return new TaxonNode(predator.getUnderlyingNode()
                .getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS))
                .iterator().next().getEndNode());
    }

    @Test
    public void interactionTypePredicateMissing() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionListenerImpl.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new HashMap<String, String>()), is(false));
    }

    @Test
    public void interactionTypePredicateInvalid() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionListenerImpl.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new HashMap<String, String>() {{
            put(StudyImporterForTSV.INTERACTION_TYPE_ID, "bla");
        }}), is(false));
    }

    @Test
    public void interactionTypePredicateValid() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionListenerImpl.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new HashMap<String, String>() {{
            put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.INTERACTS_WITH.getIRI());
        }}), is(true));
    }

    @Test
    public void throwingGeoNamesService() throws StudyImporterException {
        final List<String> msgs = new ArrayList<>();
        InteractionListenerImpl interactionListener = new InteractionListenerImpl(nodeFactory, new GeoNamesService() {

            @Override
            public boolean hasTermForLocale(String locality) {
                return true;
            }

            @Override
            public LatLng findLatLng(String locality) throws IOException {
                throw new IOException("kaboom!");
            }
        }, new ImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                msgs.add(message);
            }

            @Override
            public void info(LogContext ctx, String message) {

            }

            @Override
            public void severe(LogContext ctx, String message) {

            }
        });

        final HashMap<String, String> link = new HashMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(LOCALITY_ID, "bla:123");
        link.put(LOCALITY_NAME, "my back yard");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "doi:1234");
        try {
            interactionListener.newLink(link);
            assertThat(msgs.size(), is(1));
            assertThat(msgs.get(0), startsWith("failed to lookup [bla:123]"));
        } catch (StudyImporterException ex) {
            fail("should not throw on failing geoname lookup");
        }
    }

}
