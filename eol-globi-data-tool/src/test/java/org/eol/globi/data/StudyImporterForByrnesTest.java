package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class StudyImporterForByrnesTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporterForByrnes studyImporterForByrnes = new StudyImporterForByrnes(new ParserFactoryImpl(), nodeFactory);
        studyImporterForByrnes.importStudy();

        List<Study> studies = NodeFactory.findAllStudies(getGraphDb());
        assertTrue(studies.size() > 0);
        for (Study study : studies) {
            assertThat(study.getTitle(), is(notNullValue()));
            assertThat(study.getSource(), is(notNullValue()));
            assertThat(study.getCitation(), is(notNullValue()));
        }

        assertNotNull(nodeFactory.findTaxon("Anisotremus davidsonii"));
    }
}
