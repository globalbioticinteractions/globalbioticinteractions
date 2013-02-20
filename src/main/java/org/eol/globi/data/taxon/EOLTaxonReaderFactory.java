package org.eol.globi.data.taxon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class EOLTaxonReaderFactory implements TaxonReaderFactory {

    @Override
    public BufferedReader createReader() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("eol/taxon.tab.gz");
        GZIPInputStream gzipInputStream = new GZIPInputStream(resourceAsStream);
        return new BufferedReader(new InputStreamReader(gzipInputStream));
    }
}
