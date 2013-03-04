package org.eol.globi.data.taxon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public  class OboTaxonReaderFactory implements TaxonReaderFactory {
    @Override
    public BufferedReader createReader() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/org/obofoundry/ncbi_taxonomy.obo.gz");
        GZIPInputStream gzipInputStream = new GZIPInputStream(resourceAsStream);
        return new BufferedReader(new InputStreamReader(gzipInputStream));
    }
}
