package org.eol.globi.export;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryImpl;
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

public class ExporterGoMexSITest extends GraphDBTestCase {

    @Test
    public void testQuery() {
        StringBuilder query = ExporterGoMexSI.buildQuery();
        assertThat(query.toString(), is(notNullValue()));
    }

    @Test
    public void testHeader() throws IOException {
        StringWriter writer = new StringWriter();
        Study study = nodeFactory.getOrCreateStudy("bla", "boo", "boo");
        new ExporterGoMexSI().exportStudy(study, writer, true);

        assertHeader(new LabeledCSVParser(new CSVParser(IOUtils.toInputStream(writer.toString()))));
    }

    @Test
    public void exportAll() throws StudyImporterException, IOException {
        StudyImporter importer = new StudyImporterForSimons(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();
        List<Study> allStudies = NodeFactoryImpl.findAllStudies(getGraphDb());

        assertThat(allStudies.size() > 0, is(true));

        StringWriter writer = new StringWriter();
        boolean isFirst = true;
        for (Study study : allStudies) {
            new ExporterGoMexSI().exportStudy(study, writer, isFirst);
            isFirst = false;
        }

        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(IOUtils.toInputStream(writer.toString())));
        assertHeader(parser);
        String line[];
        while ((line = parser.getLine()) != null) {
            assertThat("found line [" + parser.lastLineNumber() + "] that was too short [" + StringUtils.join(line, ",") + "]", line.length, is(parser.getLabels().length));
        }

    }

    private void assertHeader(LabeledCSVParser parser) throws IOException {
        assertThat(parser.getLabels(), is(new String[]{"predator taxon name", "predator taxon ids", "prey taxon name", "prey taxon id", "observation time (unix time)", "latitude", "longitude", "depth(m)", "environment names", "environment ids", "ecoregion names", "ecoregion ids", "study ref"}));
    }
}
