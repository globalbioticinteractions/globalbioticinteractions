package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.TaxonNode;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForSeltmannTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, NodeFactoryException, IOException {
        TestParserFactory parserFactory = new TestParserFactory(new HashMap<String, String>() {
            {
                put("associatedTaxa.tsv", IOUtils.toString(getClass().getResourceAsStream("seltmann/associatedTaxa.tsv"), "UTF-8"));
                put("occurrences.tsv", IOUtils.toString(getClass().getResourceAsStream("seltmann/occurrences.tsv"), "UTF-8"));
            }
        });

        StudyImporterForSeltmann importer = new StudyImporterForSeltmann(parserFactory, nodeFactory);
        importer.importStudy();

        assertThat(nodeFactory.findTaxonByName("Megandrena mentzeliae"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Mentzelia tricuspis"), is(notNullValue()));

    }
}
