package org.eol.globi.data.taxon;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class EOLTaxonParserTest {

    @Test
    public void readLine() throws IOException {
        TaxonReaderFactory taxonReaderFactory = new EOLTaxonReaderFactory();
        assertThat(taxonReaderFactory.createReader(), is(notNullValue()));


        TaxonParser taxonParser = new EOLTaxonParser();
        final List<TaxonTerm> terms = new ArrayList<TaxonTerm>();
        TestTaxonImportListener listener = new TestTaxonImportListener(terms);
        taxonParser.parse(taxonReaderFactory.createReader(), listener);

        TaxonTerm taxonTerm = terms.get(0);
        assertThat(taxonTerm.getId(), is("1"));
        assertThat(taxonTerm.getRank(), is(nullValue()));
        assertThat(taxonTerm.getName(), is("Animalia"));
        assertThat(terms.size(), is(10));

        assertThat(listener.count, is(taxonParser.getExpectedMaxTerms()));

    }

    private static class TestTaxonImportListener implements TaxonImportListener {
        private final List<TaxonTerm> terms;
        int count = 0;

        public TestTaxonImportListener(List<TaxonTerm> terms) {
            this.terms = terms;
        }

        @Override
        public void addTerm(String name, long id) {
            if (terms.size() < 10) {
                TaxonTerm taxonTerm = new TaxonTerm();
                taxonTerm.setName(name);
                taxonTerm.setId(Long.toString(id));
                terms.add(taxonTerm);
            }
            count++;
        }

        @Override
        public void start() {

        }

        @Override
        public void finish() {

        }
    }

}
