package org.trophic.graph.data.taxon;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        TestTaxonTermListener listener = new TestTaxonTermListener(terms);
        taxonParser.parse(taxonReaderFactory.createReader(), listener);

        TaxonTerm taxonTerm = terms.get(0);
        assertThat(taxonTerm.getId(), is("EOL:1"));
        assertThat(taxonTerm.getRank(), is("kingdom"));
        assertThat(taxonTerm.getName(), is("Animalia"));
        assertThat(terms.size(), is(10));

        assertThat(listener.count, is(taxonParser.getExpectedMaxTerms()));

    }

    private static class TestTaxonTermListener implements TaxonTermListener {
        private final List<TaxonTerm> terms;
        int count = 0;

        public TestTaxonTermListener(List<TaxonTerm> terms) {
            this.terms = terms;
        }

        @Override
        public void notifyTerm(TaxonTerm term) {
            if (terms.size() < 10) {
                terms.add(term);
            }
            count++;
        }
    }

}
