package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForBroseIT extends GraphDBTestCase {

    @Override
    protected TermLookupService getTermLookupService() {
        return new UberonLookupService();
    }

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporterForBrose studyImporterForBrose = new DatasetImporterForBrose(new ParserFactoryLocal(), nodeFactory);
        studyImporterForBrose.importStudy();

        List<StudyNode> studies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(studies.size(), is(20));
        for (Study study : studies) {
            assertThat(study.getTitle(), is(notNullValue()));
            assertThat(StringUtils.isBlank(study.getCitation()), is(false));
        }
    }

}
