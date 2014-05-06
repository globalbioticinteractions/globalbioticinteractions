package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSimons;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ExporterGoMexSITest extends GraphDBTestCase {

    @Test
    public void testQuery() {
        StringBuilder query = ExporterGoMexSI.buildQuery();
        assertThat(query.toString(), is(notNullValue()));
    }

    @Test
    public void exportAll() throws StudyImporterException, IOException {
        StudyImporter importer = new StudyImporterForSimons(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();
        List<Study> allStudies = NodeFactory.findAllStudies(getGraphDb());

        assertThat(allStudies.size() > 0, is(true));

        StringWriter writer = new StringWriter();
        boolean isFirst = true;
        for (Study study : allStudies) {
            new ExporterGoMexSI().exportStudy(study, writer, isFirst);
            isFirst = false;
        }

        assertThat(writer.toString(), containsString("Ariopsis felis"));


    }
}
