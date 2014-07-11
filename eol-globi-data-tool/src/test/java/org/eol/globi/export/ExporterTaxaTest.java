package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.taxon.TaxonService;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.nullValue;
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

        String actual = exportStudy(myStudy1);
        assertThat(actual, containsString("EOL:123,Canis lupus,,,,,,,,,http://eol.org/pages/123,,,,"));
        assertThat(actual, containsString("EOL:45634,Homo sapiens,,,,,,,,,http://eol.org/pages/45634,,,,"));
        assertThat(actual, not(containsString("no:match,ThemFishes,,,,,,,,,,,,,")));

        assertThatNoTaxaAreExportedOnMissingHeader(myStudy1, new StringWriter());
    }

    protected String exportStudy(Study myStudy1) throws IOException {
        StringWriter row = new StringWriter();

        new ExporterTaxa().exportStudy(myStudy1, row, true);

        return row.getBuffer().toString();
    }

    @Test
    public void excludeNoMatchNames() throws NodeFactoryException, IOException {
        Specimen predator = nodeFactory.createSpecimen(PropertyAndValueDictionary.NO_MATCH, "EOL:1234");
        Specimen prey = nodeFactory.createSpecimen(PropertyAndValueDictionary.NO_MATCH, "EOL:122");
        Study study = nodeFactory.createStudy("bla");
        study.collected(predator);
        predator.ate(prey);
        assertThat(exportStudy(study), not(containsString(PropertyAndValueDictionary.NO_MATCH)));
    }


    @Test
    public void includeHigherOrderRanks() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("rank", "the taxon rank");
                put("pathNames", "kingdom | phylum | class | order | family | genus");
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("scientificName", "Some namus");
                put("taxonId", "EOL:1234");
            }
        };
        HashMap<String, String> rowFields = new HashMap<String, String>();
        ExporterTaxa.resultsToRow(rowFields, result);

        assertThat(rowFields.get(EOLDictionary.TAXON_ID), is("EOL:1234"));
        assertThat(rowFields.get(EOLDictionary.SCIENTIFIC_NAME), is("Some namus"));
        assertThat(rowFields.get(EOLDictionary.TAXON_RANK), is("the taxon rank"));
        assertThat(rowFields.get(EOLDictionary.KINGDOM), is("the kingdom"));
        assertThat(rowFields.get(EOLDictionary.PHYLUM), is("the phylum"));
        assertThat(rowFields.get(EOLDictionary.CLASS), is("the class"));
        assertThat(rowFields.get(EOLDictionary.ORDER), is("the order"));
        assertThat(rowFields.get(EOLDictionary.FAMILY), is("the family"));
        assertThat(rowFields.get(EOLDictionary.GENUS), is("the genus"));
        assertThat(rowFields.get(EOLDictionary.FURTHER_INFORMATION_URL), is("http://eol.org/pages/1234"));
    }

    @Test
    public void missingTaxonPathnames() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("taxonRankz", "the taxon rank");
                put("pathNames", null);
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("scientificName", "Some namus");
                put("taxonId", "ZZZ:1234");
            }
        };
        HashMap<String, String> rowFields = new HashMap<String, String>();
        ExporterTaxa.resultsToRow(rowFields, result);

        assertThat(rowFields.get(EOLDictionary.TAXON_ID), is("ZZZ:1234"));
        assertThat(rowFields.get(EOLDictionary.SCIENTIFIC_NAME), is("Some namus"));
        assertThat(rowFields.containsKey(EOLDictionary.TAXON_RANK), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.KINGDOM), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.PHYLUM), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.CLASS), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.ORDER), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.FAMILY), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.GENUS), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.FURTHER_INFORMATION_URL), is(false));
    }
@Test
    public void includeInvalidHigherOrderRanks() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("taxonRankz", "the taxon rank");
                put("pathNames", "kingdomz | phylum | classez | orderz | family | genuszz");
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("scientificName", "Some namus");
                put("taxonId", "ZZZ:1234");
            }
        };
        HashMap<String, String> rowFields = new HashMap<String, String>();
        ExporterTaxa.resultsToRow(rowFields, result);

        assertThat(rowFields.get(EOLDictionary.TAXON_ID), is("ZZZ:1234"));
        assertThat(rowFields.get(EOLDictionary.SCIENTIFIC_NAME), is("Some namus"));
        assertThat(rowFields.containsKey(EOLDictionary.TAXON_RANK), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.KINGDOM), is(false));
        assertThat(rowFields.get(EOLDictionary.PHYLUM), is("the phylum"));
        assertThat(rowFields.containsKey(EOLDictionary.CLASS), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.ORDER), is(false));
        assertThat(rowFields.get(EOLDictionary.FAMILY), is("the family"));
        assertThat(rowFields.containsKey(EOLDictionary.GENUS), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.FURTHER_INFORMATION_URL), is(false));
    }

    private void assertThatNoTaxaAreExportedOnMissingHeader(Study myStudy1, StringWriter row) throws IOException {
        new ExporterTaxa().exportStudy(myStudy1, row, false);
        assertThat(row.getBuffer().toString(), is(""));
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExporterTaxa exporter = new ExporterTaxa();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}