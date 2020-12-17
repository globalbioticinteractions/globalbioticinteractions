package org.eol.globi.process;

import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_COLLECTION_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.fail;

public class InteractionListenerImplTest extends GraphDBTestCase {

    private void handleRelations(NodeUtil.RelationshipListener handler, RelTypes collected) {
        final List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        final StudyNode study = allStudies.get(0);
        assertThat(study.getCitation(), is(""));
        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode(), collected), handler, 1);
    }

    @Test
    public void importWithMissingTargetTaxonButAvailableInstitutionCollectionCatalogTriple() throws StudyImporterException {
        List<String> msgs = new ArrayList<>();
        final InteractionListener listener = new InteractionListenerImpl(nodeFactory,
                null,
                new NullImportLogger() {
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
        link.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        link.put(TARGET_OCCURRENCE_ID, "occurrenceId123");
        link.put(TARGET_INSTITUTION_CODE, "institutionCode123");
        link.put(TARGET_COLLECTION_CODE, "collectionCode123");
        link.put(TARGET_COLLECTION_ID, "collectionId123");
        link.put(TARGET_CATALOG_NUMBER, "catalogNumber123");
        link.put(DATASET_CITATION, "some source ref");
        link.put(REFERENCE_ID, "123");
        link.put(REFERENCE_CITATION, "");

        listener.on(link);
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
        final InteractionListener listener = new InteractionListenerImpl(nodeFactory,
                null,
                new NullImportLogger() {
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
        link.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        link.put(SOURCE_OCCURRENCE_ID, "occurrenceId123");
        link.put(SOURCE_INSTITUTION_CODE, "institutionCode123");
        link.put(SOURCE_COLLECTION_CODE, "collectionCode123");
        link.put(SOURCE_COLLECTION_ID, "collectionId123");
        link.put(SOURCE_CATALOG_NUMBER, "catalogNumber123");
        link.put(DATASET_CITATION, "some source ref");
        link.put(REFERENCE_ID, "123");
        link.put(REFERENCE_CITATION, "");

        listener.on(link);
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

    public TaxonNode getOrigTaxon(SpecimenNode predator) {
        return new TaxonNode(predator.getUnderlyingNode()
                .getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS))
                .iterator().next().getEndNode());
    }


    @Test
    public void malformedDOI() {
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
        link.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(REFERENCE_ID, "123");
        link.put(DATASET_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "bla:XXX");
        try {
            interactionListener.on(link);
            assertThat(msgs.size(), is(1));
            assertThat(msgs.get(0), startsWith("found malformed doi [bla:XXX]"));
        } catch (StudyImporterException ex) {
            fail("should not throw on failing geoname lookup");
        }
    }

}
