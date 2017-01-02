package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ExporterTaxaDistinctTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        ExportTestUtil.createTestData(null, nodeFactory);
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Canis lupus", "EOL:123"));
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Canis", "EOL:126"));
        taxonIndex.getOrCreateTaxon(new TaxonImpl("ThemFishes", "no:match"));
        resolveNames();

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        String actual = exportStudy(myStudy1);
        assertThat(actual, containsString("EOL:123\tCanis lupus\t\t\t\t\t\t\t\t\thttp://eol.org/pages/123\t\t\t\t"));
        assertThat(actual, containsString("EOL:45634\tHomo sapiens\t\t\t\t\t\t\t\t\thttp://eol.org/pages/45634\t\t\t\t"));
        assertThat(actual, not(containsString("no:match\tThemFishes\t\t\t\t\t\t\t\t\t\t\t\t\t")));

        assertThatNoTaxaAreExportedOnMissingHeader(myStudy1, new StringWriter());
    }

    protected String exportStudy(Study myStudy1) throws IOException {
        StringWriter row = new StringWriter();
        new ExporterTaxaDistinct().exportStudy(myStudy1, row, true);
        return row.getBuffer().toString();
    }

    @Test
    public void excludeNoMatchNames() throws NodeFactoryException, IOException {
        Study study = nodeFactory.createStudy(new StudyImpl("bla", null, null, null));
        Specimen predator = nodeFactory.createSpecimen(study, new TaxonImpl(PropertyAndValueDictionary.NO_MATCH, "EOL:1234"));
        Specimen prey = nodeFactory.createSpecimen(study, new TaxonImpl(PropertyAndValueDictionary.NO_MATCH, "EOL:122"));
        predator.ate(prey);
        assertThat(exportStudy(study), not(containsString(PropertyAndValueDictionary.NO_MATCH)));
    }

    private void assertThatNoTaxaAreExportedOnMissingHeader(Study myStudy1, StringWriter row) throws IOException {
        new ExporterTaxaDistinct().exportStudy(myStudy1, row, false);
        assertThat(row.getBuffer().toString(), is(""));
    }

    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(new ExporterTaxaDistinct());
    }

}