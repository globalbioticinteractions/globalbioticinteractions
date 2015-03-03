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

        assertThat(row.getBuffer().toString().trim(), equalTo(getExpectedData()));

        row = new StringWriter();

        new ExporterReferences().exportStudy(myStudy, row, false);

        assertThat(row.getBuffer().toString().trim(), equalTo(getExpectedRow()));
    }

    @Test
    public void exportReferenceNoDescription() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        StringWriter row = new StringWriter();
        new ExporterReferences().exportStudy(myStudy, row, false);
        assertThat(row.getBuffer().toString().trim(), equalTo("globi:ref:1,,myStudy,,,,,,,,,,,,,,,"));
    }

    @Test
    public void exportReferenceEscapeCharacters() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        myStudy.setCitationWithTx("bla \"one\"");
        StringWriter row = new StringWriter();
        new ExporterReferences().exportStudy(myStudy, row, false);
        assertThat(row.getBuffer().toString().trim(), equalTo("globi:ref:1,,\"bla \"\"one\"\"\",,,,,,,,,,,,,,,"));
    }


    private String getExpectedData() {
        return "\"identifier\",\"publicationType\",\"full_reference\",\"primaryTitle\",\"title\",\"pages\",\"pageStart\",\"pageEnd\",\"volume\",\"edition\",\"publisher\",\"authorList\",\"editorList\",\"created\",\"language\",\"uri\",\"doi\",\"schema#localityName\""
                + "\n" + getExpectedRow();
    }

    private String getExpectedRow() {
        return "globi:ref:1,,citation:doi:John Doe. 1927. description study 1,,,,,,,,,,,,,http://research.myfwc.com/game/Survey.aspx?id=444,doi:1234,";
    }

    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExporterReferences exporter = new ExporterReferences();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}
