package org.eol.globi.opentree;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class OpenTreeUtilTest {

    @Test
    public void readNewick() {
        TestTaxonListener listener = new TestTaxonListener();

        InputStream inputStream = getClass().getResourceAsStream("/draftversion1.tre");
        assertThat(inputStream, is(notNullValue()));

        Pattern idPattern = Pattern.compile("_ott[0-9]+");
        Scanner scanner = new Scanner(inputStream, "UTF-8");
        scanner.useDelimiter("[\\),]");


        String taxonId;
        while ((taxonId = scanner.findWithinHorizon(idPattern, 0)) != null) {
            listener.addTaxonId(taxonId);
        }
        assertThat(listener.getCount(), is (2426905));

    }

    @Test
    public void readTaxonomy() throws IOException {
        TestTaxonListener testTaxonListener = new TestTaxonListener();
        InputStream inputStream = getClass().getResourceAsStream("/ott/taxonomy.tsv");
        assertThat(inputStream, is(notNullValue()));
        OpenTreeUtil.readTaxonomy(testTaxonListener, inputStream);

        assertThat(testTaxonListener.getCount(), is(3307104));
    }

    private class TestTaxonListener implements TaxonListener {
        private int count = 0;


        @Override
        public void addTaxonId(String taxonId) {
            count++;
        }

        @Override
        public void taxonSameAs(String taxonId, String sameAsIds) {
            count++;
        }

        public int getCount() {
            return count;
        }
    }
}
