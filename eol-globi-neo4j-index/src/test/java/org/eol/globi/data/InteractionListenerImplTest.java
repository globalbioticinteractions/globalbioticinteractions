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
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.eol.globi.data.StudyImporterForTSV.SOURCE_COLLECTION_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_COLLECTION_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.COLLECTION_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET;
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
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_CATALOG_NUMBER;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_COLLECTION_CODE;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_INSTITUTION_CODE;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_LIFE_STAGE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_SEX_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_SEX_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_NAMES;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_CATALOG_NUMBER;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_COLLECTION_CODE;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_INSTITUTION_CODE;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_SEX_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_SEX_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.domain.PropertyAndValueDictionary.CATALOG_NUMBER;
import static org.eol.globi.domain.PropertyAndValueDictionary.COLLECTION_CODE;
import static org.eol.globi.domain.PropertyAndValueDictionary.INSTITUTION_CODE;
import static org.eol.globi.domain.PropertyAndValueDictionary.OCCURRENCE_ID;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InteractionListenerImplTest extends GraphDBTestCase {

    @Test
    public void importBlankCitation() throws StudyImporterException {
        final InteractionListenerImpl listener = new InteractionListenerImpl(nodeFactory, null, null);
        final TreeMap<String, String> link = new TreeMap<String, String>();
        link.put(SOURCE_OCCURRENCE_ID, "123");
        link.put(SOURCE_CATALOG_NUMBER, "catalogNumber123");
        link.put(SOURCE_COLLECTION_CODE, "collectionCode123");
        link.put(SOURCE_COLLECTION_ID, "collectionId123");
        link.put(SOURCE_INSTITUTION_CODE, "institutionCode123");
        link.put(TARGET_OCCURRENCE_ID, "456");
        link.put(TARGET_CATALOG_NUMBER, "targetCatalogNumber123");
        link.put(TARGET_COLLECTION_CODE, "targetCollectionCode123");
        link.put(TARGET_COLLECTION_ID, "targetCollectionId123");
        link.put(TARGET_INSTITUTION_CODE, "targetInstitutionCode123");
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

                assertThat(predator.getProperty(OCCURRENCE_ID), is("123"));
                assertThat(predator.getProperty(CATALOG_NUMBER), is("catalogNumber123"));
                assertThat(predator.getProperty(COLLECTION_CODE), is("collectionCode123"));
                assertThat(predator.getProperty(COLLECTION_ID), is("collectionId123"));
                assertThat(predator.getProperty(INSTITUTION_CODE), is("institutionCode123"));

                assertThat(prey.getProperty(OCCURRENCE_ID), is("456"));
                assertThat(prey.getProperty(CATALOG_NUMBER), is("targetCatalogNumber123"));
                assertThat(prey.getProperty(COLLECTION_CODE), is("targetCollectionCode123"));
                assertThat(prey.getProperty(COLLECTION_ID), is("targetCollectionId123"));
                assertThat(prey.getProperty(INSTITUTION_CODE), is("targetInstitutionCode123"));
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
        final StudyNode study = allStudies.get(0);
        assertThat(study.getCitation(), is(""));
        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode(), collected), handler, 1);
    }

    public InteractionListenerImpl getAssertingListener() {
        return new InteractionListenerImpl(nodeFactory, null, new NullImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                fail("got message: " + message);
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
        final TreeMap<String, String> link = new TreeMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_TAXON_ID, "duck");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(TARGET_TAXON_ID, "mouse");
        link.put(ARGUMENT_TYPE_ID, "https://en.wiktionary.org/wiki/refute");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "doi:10.12/34");
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
        final TreeMap<String, String> link = new TreeMap<>();
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
        link.put(REFERENCE_DOI, "doi:10.12/34");
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
    public void importWithSex() throws StudyImporterException {
        final InteractionListenerImpl listener = getAssertingListener();
        final TreeMap<String, String> link = new TreeMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_SEX_NAME, "female");
        link.put(SOURCE_SEX_ID, "some:female");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(TARGET_SEX_NAME, "male");
        link.put(TARGET_SEX_ID, "some:male");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_ID, "123");
        link.put(REFERENCE_CITATION, "");
        listener.newLink(link);

        AtomicInteger foundSpecimen = new AtomicInteger(0);
        NodeUtil.RelationshipListener someListener = relationship -> {
            final SpecimenNode someSpecimen = new SpecimenNode(relationship.getEndNode());
            assertTrue(someSpecimen.getUnderlyingNode().hasRelationship(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.COLLECTED)));
            assertTrue(someSpecimen.getUnderlyingNode().hasRelationship(NodeUtil.asNeo4j(InteractType.ATE)));

            if (someSpecimen.getUnderlyingNode().hasRelationship(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE))) {
                assertThat(someSpecimen.getSex().getName(), is("female"));
                assertThat(someSpecimen.getSex().getId(), is("some:female"));
            } else {
                assertThat(someSpecimen.getSex().getName(), is("male"));
                assertThat(someSpecimen.getSex().getId(), is("some:male"));
            }
            foundSpecimen.incrementAndGet();
        };

        handleRelations(someListener, RelTypes.COLLECTED);

        assertThat(foundSpecimen.get(), is(2));
    }

    @Test
    public void importWithTaxonHierarchy() throws StudyImporterException {
        final InteractionListenerImpl listener = getAssertingListener();
        final TreeMap<String, String> link = new TreeMap<>();
        link.put(SOURCE_TAXON_NAME, "Donald duck");
        link.put(SOURCE_TAXON_PATH, "Aves | Donald | Donald duck");
        link.put(SOURCE_TAXON_PATH_NAMES, "class | genus | species");
        link.put(SOURCE_TAXON_SPECIFIC_EPITHET, "duck");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_ID, "123");
        link.put(REFERENCE_CITATION, "");
        listener.newLink(link);

        AtomicInteger foundSpecimen = new AtomicInteger(0);
        NodeUtil.RelationshipListener someListener = relationship -> {
            final SpecimenNode someSpecimen = new SpecimenNode(relationship.getEndNode());
            assertTrue(someSpecimen.getUnderlyingNode().hasRelationship(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.COLLECTED)));
            assertTrue(someSpecimen.getUnderlyingNode().hasRelationship(NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS)));

            Node taxonNode = someSpecimen
                    .getUnderlyingNode()
                    .getSingleRelationship(NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS), Direction.OUTGOING)
                    .getEndNode();

            TaxonNode taxon = new TaxonNode(taxonNode);

            if (someSpecimen.getUnderlyingNode().hasRelationship(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE))) {
                assertThat(taxon.getPath(), is("Aves | Donald | Donald duck"));
                assertThat(taxon.getPathNames(), is("class | genus | species"));
                foundSpecimen.incrementAndGet();
            }

        };

        handleRelations(someListener, RelTypes.COLLECTED);

        assertThat(foundSpecimen.get(), is(1));
    }

    @Test
    public void importWithMissingTargetTaxonButAvailableInstitutionCollectionCatalogTriple() throws StudyImporterException {
        List<String> msgs = new ArrayList<>();
        final InteractionListenerImpl listener = new InteractionListenerImpl(nodeFactory, null, new NullImportLogger() {
            @Override
            public void info(LogContext ctx, String message) {
                msgs.add(message);
            }

            @Override
            public void warn(LogContext ctx, String message) {
                msgs.add(message);
            }

            @Override
            public void severe(LogContext ctx, String message) {
                msgs.add(message);
            }
        });
        final TreeMap<String, String> link = new TreeMap<>();
        link.put(SOURCE_TAXON_NAME, "Donald duck");
        link.put(SOURCE_TAXON_PATH, "Aves | Donald | Donald duck");
        link.put(SOURCE_TAXON_PATH_NAMES, "class | genus | species");
        link.put(SOURCE_TAXON_SPECIFIC_EPITHET, "duck");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        link.put(TARGET_OCCURRENCE_ID, "occurrenceId123");
        link.put(TARGET_INSTITUTION_CODE, "institutionCode123");
        link.put(TARGET_COLLECTION_CODE, "collectionCode123");
        link.put(TARGET_COLLECTION_ID, "collectionId123");
        link.put(TARGET_CATALOG_NUMBER, "catalogNumber123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_ID, "123");
        link.put(REFERENCE_CITATION, "");

        listener.newLink(link);
        assertThat(msgs, hasItem("target taxon name missing: using institutionCode/collectionCode/collectionId/catalogNumber/occurrenceId as placeholder"));

        final AtomicBoolean foundPair = new AtomicBoolean(false);
        NodeUtil.RelationshipListener relationshipListener = relationship -> {
            final SpecimenNode predator = new SpecimenNode(relationship.getEndNode());
            for (Relationship stomachRel : NodeUtil.getStomachContents(predator)) {
                final SpecimenNode prey = new SpecimenNode(stomachRel.getEndNode());
                final TaxonNode preyTaxon = getOrigTaxon(prey);

                assertThat(preyTaxon.getName(), is("institutionCode123 | collectionCode123 | collectionId123 | catalogNumber123 | occurrenceId123"));
                foundPair.set(true);
            }
        };


        handleRelations(relationshipListener, RelTypes.COLLECTED);
        assertThat(foundPair.get(), is(true));

    }

    @Test
    public void importWithMissingSourceTaxonButAvailableInstitutionCollectionCatalogTriple() throws StudyImporterException {
        List<String> msgs = new ArrayList<>();
        final InteractionListenerImpl listener = new InteractionListenerImpl(nodeFactory, null, new NullImportLogger() {
            @Override
            public void info(LogContext ctx, String message) {
                msgs.add(message);
            }

            @Override
            public void warn(LogContext ctx, String message) {
                msgs.add(message);
            }

            @Override
            public void severe(LogContext ctx, String message) {
                msgs.add(message);
            }
        });
        final TreeMap<String, String> link = new TreeMap<>();
        link.put(TARGET_TAXON_NAME, "Donald duck");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        link.put(SOURCE_OCCURRENCE_ID, "occurrenceId123");
        link.put(SOURCE_INSTITUTION_CODE, "institutionCode123");
        link.put(SOURCE_COLLECTION_CODE, "collectionCode123");
        link.put(SOURCE_COLLECTION_ID, "collectionId123");
        link.put(SOURCE_CATALOG_NUMBER, "catalogNumber123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_ID, "123");
        link.put(REFERENCE_CITATION, "");

        listener.newLink(link);
        assertThat(msgs, hasItem("source taxon name missing: using institutionCode/collectionCode/collectionId/catalogNumber/occurrenceId as placeholder"));

        final AtomicBoolean foundPair = new AtomicBoolean(false);
        NodeUtil.RelationshipListener relationshipListener = relationship -> {
            final SpecimenNode predator = new SpecimenNode(relationship.getEndNode());
            for (Relationship stomachRel : NodeUtil.getStomachContents(predator)) {
                final SpecimenNode pred = new SpecimenNode(stomachRel.getStartNode());
                final TaxonNode predTaxon = getOrigTaxon(predator);

                assertThat(predTaxon.getName(), is("institutionCode123 | collectionCode123 | collectionId123 | catalogNumber123 | occurrenceId123"));
                foundPair.set(true);
            }
        };


        handleRelations(relationshipListener, RelTypes.COLLECTED);
        assertThat(foundPair.get(), is(true));

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
        final TreeMap<String, String> link = new TreeMap<>();
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

        assertThat(interactionTypePredicate.test(new TreeMap<String, String>()), is(false));
    }

    @Test
    public void interactionTypePredicateInvalid() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionListenerImpl.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new TreeMap<String, String>() {{
            put(StudyImporterForTSV.INTERACTION_TYPE_ID, "bla");
        }}), is(false));
    }

    @Test
    public void interactionTypePredicateValid() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionListenerImpl.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new TreeMap<String, String>() {{
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
        }, new NullImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                msgs.add(message);
            }
        });

        final TreeMap<String, String> link = new TreeMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(LOCALITY_ID, "bla:123");
        link.put(LOCALITY_NAME, "my back yard");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        try {
            interactionListener.newLink(link);
            assertThat(msgs.size(), is(1));
            assertThat(msgs.get(0), startsWith("failed to lookup [bla:123]"));
        } catch (StudyImporterException ex) {
            fail("should not throw on failing geoname lookup");
        }
    }

    @Test
    public void malformedDOI() throws StudyImporterException {
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
        }, new NullImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                msgs.add(message);
            }

        });

        final TreeMap<String, String> link = new TreeMap<>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "bla:XXX");
        try {
            interactionListener.newLink(link);
            assertThat(msgs.size(), is(1));
            assertThat(msgs.get(0), startsWith("found malformed doi [bla:XXX]"));
        } catch (StudyImporterException ex) {
            fail("should not throw on failing geoname lookup");
        }
    }

}
