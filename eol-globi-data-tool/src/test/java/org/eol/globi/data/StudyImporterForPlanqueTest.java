package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForPlanqueTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        final List<String> errorMessages = new ArrayList<String>();


        StudyImporter importer = new StudyImporterForPlanque(new ParserFactoryImpl(), nodeFactory);
        importer.setLogger(new ImportLogger() {
            @Override
            public void warn(Study study, String message) {
                errorMessages.add(message);
            }

            @Override
            public void info(Study study, String message) {

            }

            @Override
            public void severe(Study study, String message) {

            }
        });
        importer.importStudy();

        List<Study> studies = NodeFactory.findAllStudies(getGraphDb());

        assertThat(studies.size(), is(215));
        assertThat(nodeFactory.findTaxon("Sagitta elegans"), is(notNullValue()));

        assertThat(errorMessages.size(), is(67));
    }

    @Test
    public void taxonNameParser() {
        assertThat(StudyImporterForPlanque.normalizeName("SAGITTA_ELEGANS"), is("Sagitta elegans"));
        assertThat(StudyImporterForPlanque.normalizeName("OSTRACODA_INDET"), is("Ostracoda"));
    }

}
