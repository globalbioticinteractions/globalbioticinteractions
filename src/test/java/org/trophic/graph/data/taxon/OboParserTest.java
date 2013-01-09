package org.trophic.graph.data.taxon;

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
        BufferedReader bufferedReader = new OboTaxonReaderFactory().createReader();
        this.counter = 0;

        TaxonTermListener listener = new TaxonTermListener() {

            @Override
            public void notifyTerm(TaxonTerm term) {
                if (getCounter() % 1000 == 0) {
                    LOG.info("got term with id: [" + term.getId() +
                            "], is_a: [" + term.getIsA() +
                            "], name: [" + term.getName() +
                            "], rank: [" + term.getRank() + "]");
                }
                assertThat(term.getId(), is(not(nullValue())));
                assertThat(term.getName(), is(not(nullValue())));
                assertThat(term.getRank(), is(not(nullValue())));
                count();
            }
        };

        OboParser oboParser = new OboParser();
        oboParser.parse(bufferedReader, listener);
        assertThat("expected a certain amount of terms", getCounter(), is(oboParser.getExpectedMaxTerms()));
    }


    private void count() {
        counter++;
    }

    public int getCounter() {
        return counter;
    }
}
