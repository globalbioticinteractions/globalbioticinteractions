package org.eol.globi.data.taxon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class OboParserTest {

    private static final Log LOG = LogFactory.getLog(OboParserTest.class);

    private int counter;

    @Test
    public void oboImport() throws IOException {
        BufferedReader reader = new SingleResourceTaxonReaderFactory("/org/obofoundry/ncbi_taxonomy.obo.gz").getAllReaders().values().iterator().next();
        this.counter = 0;

        TaxonImportListener listener = new TaxonImportListener() {

            @Override
            public void addTerm(TaxonTerm term) {
                assertThat(term.getName(), is(not(nullValue())));
                assertThat(Long.parseLong(term.getId()) > 0, is(true));
                count();
            }

            @Override
            public void addTerm(String name, TaxonTerm term) {

            }

            @Override
            public void start() {

            }

            @Override
            public void finish() {

            }
        };

        OboParser oboParser = new OboParser();
        oboParser.parse(reader, listener);
        assertThat("expected a certain amount of terms", getCounter(), is(oboParser.getExpectedMaxTerms()));
    }


    private void count() {
        counter++;
    }

    public int getCounter() {
        return counter;
    }
}
