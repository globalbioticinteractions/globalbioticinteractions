package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForBarnesIT extends GraphDBTestCase {

    @Override
    protected TermLookupService getTermLookupService() {
        return new UberonLookupService(new ResourceServiceLocal());
    }

    @Test
    public void importOnceInAWhile() throws StudyImporterException {
        DatasetImporterForBarnes studyImporterForBarnes = new DatasetImporterForBarnes(new ParserFactoryLocal(), nodeFactory);
        studyImporterForBarnes.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber % 50 == 0;
            }
        });
        studyImporterForBarnes.importStudy();

        List<StudyNode> studies = NodeUtil.findAllStudies(getGraphDb());
        assertTrue(studies.size() > 0);
        for (Study study : studies) {
            assertThat(study.getTitle(), is(notNullValue()));
            assertThat(StringUtils.isBlank(study.getCitation()), is(false));
        }
    }

}
