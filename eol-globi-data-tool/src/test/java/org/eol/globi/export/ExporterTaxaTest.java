package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ExporterTaxaTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        ExportTestUtil.createTestData(null, nodeFactory);
        nodeFactory.getOrCreateTaxon("Canis lupus", "EOL:123", null);
        nodeFactory.getOrCreateTaxon("Canis", "EOL:126", null);
        nodeFactory.getOrCreateTaxon("ThemFishes", "no:match", null);

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new ExporterTaxa().exportStudy(myStudy1, row, false);

        String actual = row.getBuffer().toString();
        assertThat(actual, containsString("EOL:123,Canis lupus,,,,,,,,,,,,,"));
        assertThat(actual, containsString("EOL:45634,Homo sapiens,,,,,,,,,,,,,"));
        assertThat(actual, containsString("EOL:126,Canis,,,,,,,,,,,,,"));
        assertThat(actual, not(containsString("no:match,ThemFishes,,,,,,,,,,,,,")));
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExporterTaxa exporter = new ExporterTaxa();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}