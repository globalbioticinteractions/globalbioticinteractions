package org.eol.globi.taxon;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonomyImporterTest {

    @Test
    public void stringFormat() {
        TaxonomyImporter taxonomyImporter = new TaxonomyImporter(new TaxonParser() {
            @Override
            public void parse(BufferedReader reader, TaxonImportListener listener) throws IOException {

            }
        }, new TaxonReaderFactory() {
            @Override
            public Map<String, BufferedReader> getAllReaders() throws IOException {
                return null;
            }
        });
        taxonomyImporter.setCounter(123);
        String s = taxonomyImporter.formatProgressString(12.2);

        assertThat(s, is("123 12.2 terms/s"));

        taxonomyImporter.setCounter(798595);
        s = taxonomyImporter.formatProgressString(12.2);
        assertThat(s, is("798595 12.2 terms/s"));
    }

}
