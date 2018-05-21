package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExporterReferencesTest extends GraphDBTestCase {

    @Test
    public void exportReference() throws IOException, NodeFactoryException, ParseException {
        StudyImpl myStudy1 = new StudyImpl("myStudy", "a source", "doi:1234", ExternalIdUtil.toCitation("John Doe", "description study 1", "1927"));
        myStudy1.setExternalId("GAME:444");
        Study myStudy = nodeFactory.getOrCreateStudy(myStudy1);
        StringWriter row = new StringWriter();

        new ExporterReferences().exportStudy(myStudy, row, true);

        assertThat(row.getBuffer().toString(), equalTo(getExpectedData()));

        row = new StringWriter();

        new ExporterReferences().exportStudy(myStudy, row, false);

        assertThat(row.getBuffer().toString(), equalTo(getExpectedRow()));
    }

    @Test
    public void exportReferenceNoDescription() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy(new StudyImpl("myStudy", null, null, null));
        StringWriter row = new StringWriter();
        new ExporterReferences().exportStudy(myStudy, row, false);
        assertThat(row.getBuffer().toString(), equalTo("\nglobi:ref:1\t\tmyStudy\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t"));
    }

    @Test
    public void exportReferenceEscapeCharacters() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy(new StudyImpl("myStudy", null, "doi:some/doi", "bla \"one\""));
        StringWriter row = new StringWriter();
        new ExporterReferences().exportStudy(myStudy, row, false);
        assertThat(row.getBuffer().toString(), equalTo("\nglobi:ref:1\t\tbla \"one\"\t\t\t\t\t\t\t\t\t\t\t\t\thttps://doi.org/some/doi\thttps://doi.org/some/doi\t"));
    }


    private String getExpectedData() {
        return "identifier\tpublicationType\tfull_reference\tprimaryTitle\ttitle\tpages\tpageStart\tpageEnd\tvolume\tedition\tpublisher\tauthorList\teditorList\tcreated\tlanguage\turi\tdoi\tschema#localityName"
                + getExpectedRow();
    }

    private String getExpectedRow() {
        return "\nglobi:ref:1\t\tJohn Doe. 1927. description study 1\t\t\t\t\t\t\t\t\t\t\t\t\thttps://public.myfwc.com/FWRI/GAME/Survey.aspx?id=444\thttps://doi.org/1234\t";
    }

    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(new ExporterReferences());
    }

}
