package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExporterReferencesTest extends GraphDBTestCase {

    @Test
    public void exportReference() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.getOrCreateStudy("myStudy", "a source", ExternalIdUtil.toCitation("John Doe", "description study 1", "1927"));
        myStudy.setDOIWithTx("doi:1234");
        myStudy.setExternalId("GAME:444");
        StringWriter row = new StringWriter();

        new ExporterReferences().exportStudy(myStudy, row, true);

        assertThat(row.getBuffer().toString(), equalTo(getExpectedData()));

        row = new StringWriter();

        new ExporterReferences().exportStudy(myStudy, row, false);

        assertThat(row.getBuffer().toString(), equalTo(getExpectedRow()));
    }

    @Test
    public void exportReferenceNoDescription() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        StringWriter row = new StringWriter();
        new ExporterReferences().exportStudy(myStudy, row, false);
        assertThat(row.getBuffer().toString(), equalTo("\nglobi:ref:1\t\tmyStudy\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t"));
    }

    @Test
    public void exportReferenceEscapeCharacters() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        myStudy.setCitationWithTx("bla \"one\"");
        StringWriter row = new StringWriter();
        new ExporterReferences().exportStudy(myStudy, row, false);
        assertThat(row.getBuffer().toString(), equalTo("\nglobi:ref:1\t\tbla \"one\"\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t"));
    }


    private String getExpectedData() {
        return "identifier\tpublicationType\tfull_reference\tprimaryTitle\ttitle\tpages\tpageStart\tpageEnd\tvolume\tedition\tpublisher\tauthorList\teditorList\tcreated\tlanguage\turi\tdoi\tschema#localityName"
                + getExpectedRow();
    }

    private String getExpectedRow() {
        return "\nglobi:ref:1\t\tcitation:doi:John Doe. 1927. description study 1\t\t\t\t\t\t\t\t\t\t\t\t\thttps://public.myfwc.com/FWRI/GAME/Survey.aspx?id=444\tdoi:1234\t";
    }

    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(new ExporterReferences());
    }

}
