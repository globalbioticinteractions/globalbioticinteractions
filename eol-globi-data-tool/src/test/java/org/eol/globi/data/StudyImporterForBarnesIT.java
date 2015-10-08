package org.eol.globi.data;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForBarnesIT extends GraphDBTestCase {

    @Override
    protected TermLookupService getTermLookupService() {
        return new UberonLookupService();
    }

    @Test
    public void importOnceInAWhile() throws StudyImporterException {
        StudyImporterForBarnes studyImporterForBarnes = new StudyImporterForBarnes(new ParserFactoryImpl(), nodeFactory);
        studyImporterForBarnes.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber % 50 == 0;
            }
        });
        studyImporterForBarnes.importStudy();

        List<Study> studies = NodeUtil.findAllStudies(getGraphDb());
        assertTrue(studies.size() > 0);
        for (Study study : studies) {
            assertThat(study.getTitle(), is(notNullValue()));
            assertThat(study.getSource(), is(notNullValue()));
            assertThat(StringUtils.isBlank(study.getCitation()), is(false));
        }
    }

}
