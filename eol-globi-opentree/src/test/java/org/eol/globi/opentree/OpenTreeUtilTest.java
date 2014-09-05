package org.eol.globi.opentree;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class OpenTreeUtilTest {

    @Test
    public void extractIdsFromTree() {
        TestTaxonListener listener = new TestTaxonListener();
        InputStream inputStream = getClass().getResourceAsStream("/draftversion1.tre");
        assertThat(inputStream, is(notNullValue()));
        OpenTreeUtil.extractIdsFromTree(listener, inputStream);
        assertThat(listener.getCount(), is(2426905));

    }

    @Test
    public void readTaxonomy() throws IOException {
        TestOpenTreeListener testTaxonListener = new TestOpenTreeListener();
        InputStream inputStream = getClass().getResourceAsStream("/ott/taxonomy.tsv");
        assertThat(inputStream, is(notNullValue()));
        OpenTreeUtil.readTaxonomy(testTaxonListener, inputStream);
        assertThat(testTaxonListener.getCount(), is(5510152));
    }

    private class TestTaxonListener implements TaxonListener {
        private int count = 0;


        @Override
        public void addTaxonId(String taxonId) {
            count++;
        }

        public int getCount() {
            return count;
        }
    }

    private class TestOpenTreeListener implements OpenTreeListener {
        private int count = 0;


        @Override
        public void taxonSameAs(String ottId, String nonOttId) {
            count++;
        }

        public int getCount() {
            return count;
        }
    }



}
