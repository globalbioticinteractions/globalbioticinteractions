package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

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
import static org.junit.Assert.assertThat;

public class InteractionListenerNeo4jTest extends GraphDBTestCase {

    @Test
    public void importBlankCitation() throws StudyImporterException {
        final InteractionListenerNeo4j listener = new InteractionListenerNeo4j(nodeFactory, null, null);
        final HashMap<String, String> link = new HashMap<String, String>();
        link.put(SOURCE_TAXON_NAME, "donald");
        link.put(SOURCE_TAXON_ID, "duck");
        link.put(TARGET_TAXON_NAME, "mini");
        link.put(TARGET_TAXON_ID, "mouse");
        link.put(REFERENCE_ID, "123");
        link.put(STUDY_SOURCE_CITATION, "some source ref");
        link.put(REFERENCE_CITATION, "");
        link.put(REFERENCE_DOI, "doi:1234");
        listener.newLink(link);

        final List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(allStudies.get(0).getCitation(), is("citation:doi:1234"));

    }

}
