package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExporterReferencesTest extends GraphDBTestCase {

    @Test
    public void exportReference() throws IOException, NodeFactoryException, ParseException {
        StudyImpl myStudy1 = new StudyImpl("myStudy", "a source", new DOI("1234", "44"), ExternalIdUtil.toCitation("John Doe", "description study 1", "1927"));
        myStudy1.setExternalId("GAME:444");
        Study myStudy = nodeFactory.getOrCreateStudy(myStudy1);
        StringWriter row = new StringWriter();

        new ExporterReferences().exportStudy(myStudy, ExportUtil.AppenderWriter.of(row), true);

        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString().split("\n"), getExpectedData());

        row = new StringWriter();

        new ExporterReferences().exportStudy(myStudy, ExportUtil.AppenderWriter.of(row), false);

        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString(), getExpectedRow());
    }

    @Test
    public void exportReferenceNoDescription() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy(new StudyImpl("myStudy", null, null, null));
        StringWriter row = new StringWriter();
        new ExporterReferences().exportStudy(myStudy, ExportUtil.AppenderWriter.of(row), false);
        String expected = "globi:ref:X\t\tmyStudy\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";
        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString().split("\n"), expected.split("\n"));
    }

    @Test
    public void exportReferenceEscapeCharacters() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy(new StudyImpl("myStudy", null, new DOI("some", "doi"), "bla \"one\""));
        StringWriter row = new StringWriter();
        new ExporterReferences().exportStudy(myStudy, ExportUtil.AppenderWriter.of(row), false);
        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString(), "globi:ref:X\t\tbla \"one\"\t\t\t\t\t\t\t\t\t\t\t\t\thttps://doi.org/10.some/doi\t10.some/doi\t\n");
    }


    private String[] getExpectedData() {
        return ("identifier\tpublicationType\tfull_reference\tprimaryTitle\ttitle\tpages\tpageStart\tpageEnd\tvolume\tedition\tpublisher\tauthorList\teditorList\tcreated\tlanguage\turi\tdoi\tschema#localityName\n"
                + getExpectedRow()).split("\n");
    }

    private String getExpectedRow() {
        return "globi:ref:X\t\tJohn Doe. 1927. description study 1\t\t\t\t\t\t\t\t\t\t\t\t\thttps://public.myfwc.com/FWRI/GAME/Survey.aspx?id=444\t10.1234/44\t\n";
    }

    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(new ExporterReferences());
    }

}
