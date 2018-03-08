package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_BODY_PART_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_BODY_PART_NAME;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_NAME;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_NAME;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class InteractionListenerImplTest extends GraphDBTestCase {

    @Test
    public void importBlankCitation() throws StudyImporterException {
        final InteractionListenerImpl listener = new InteractionListenerImpl(nodeFactory, null, null);
        final HashMap<String, String> link = new HashMap<String, String>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_TAXON_ID, "duck");
        link.put(SOURCE_BODY_PART_ID, "bla:123");
        link.put(SOURCE_BODY_PART_NAME, "snout");
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

        final List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        final Study study = allStudies.get(0);
        assertThat(study.getCitation(), is(""));

        boolean foundPair = false;
        for (Relationship specimenRel : NodeUtil.getSpecimens(study)) {
            final SpecimenNode predator = new SpecimenNode(specimenRel.getEndNode());
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
                foundPair = true;

                assertThat(specimenRel.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH), is(notNullValue()));

                List<SpecimenNode> specimens = Arrays.asList(predator, prey);
                for (SpecimenNode specimen : specimens) {
                    assertThat(specimen.getBodyPart().getId(), is(notNullValue()));
                    assertThat(specimen.getBodyPart().getName(), is(notNullValue()));
                }
            }


        }
        assertThat(foundPair, is(true));
    }

    @Test
    public void importAssociatedTaxa() throws StudyImporterException {
        final InteractionListenerImpl listener = new InteractionListenerImpl(nodeFactory, null, new ImportLogger() {
            @Override
            public void warn(LogContext study, String message) {
                fail("got message: " + message);
            }

            @Override
            public void info(LogContext study, String message) {

            }

            @Override
            public void severe(LogContext study, String message) {
                fail("got message: " + message);
            }
        });
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

        final List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        final Study study = allStudies.get(0);
        assertThat(study.getCitation(), is(""));

        boolean foundPair = false;
        for (Relationship specimenRel : NodeUtil.getSpecimens(study)) {
            final SpecimenNode predator = new SpecimenNode(specimenRel.getEndNode());
            for (Relationship hosts : ((NodeBacked) predator).getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(InteractType.PARASITE_OF), Direction.OUTGOING)) {
                final SpecimenNode host = new SpecimenNode(hosts.getEndNode());
                final TaxonNode hostTaxon = getOrigTaxon(host);
                final TaxonNode predTaxon = getOrigTaxon(predator);
                assertThat(hostTaxon.getName(), is("Mini mouse"));
                assertThat(predTaxon.getName(), is("donald"));
                assertThat(predTaxon.getExternalId(), is("duck"));

                foundPair = true;
            }


        }
        assertThat(foundPair, is(true));
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

}
