package org.eol.globi.data.taxon;

import org.junit.Test;
import org.eol.globi.data.GraphDBTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonomyImporterTest extends GraphDBTestCase {

    @Test
    public void stringFormat() {
        TaxonomyImporter taxonomyImporter = new TaxonomyImporter(nodeFactory);
        taxonomyImporter.setCounter(123);
        String s = taxonomyImporter.formatProgressString(12.2);

        assertThat(s, is("123 (0.0%), 12.2 terms/s"));

        taxonomyImporter.setCounter(taxonomyImporter.getParser().getExpectedMaxTerms());
        s = taxonomyImporter.formatProgressString(12.2);
        assertThat(s, is("798595 (100.0%), 12.2 terms/s"));
    }

}
