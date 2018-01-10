package org.eol.globi.data;

import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.List;

import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_NAME;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_NAME;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class InteractionListenerImplTest extends GraphDBTestCase {

    @Test
    public void importBlankCitation() throws StudyImporterException {
        final InteractionListenerImpl listener = new InteractionListenerImpl(nodeFactory, null, null);
        final HashMap<String, String> link = new HashMap<String, String>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_TAXON_ID, "duck");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(TARGET_TAXON_ID, "mouse");
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
