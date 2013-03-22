package org.eol.globi.data.taxon;

import org.eol.globi.data.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;

public class EOLTaxonReaderFactory implements TaxonReaderFactory {

    @Override
    public BufferedReader createReader() throws IOException {
        return FileUtils.getBufferedReaderUTF_8(getClass().getResourceAsStream("eol/taxon.tab.gz"));
    }
}
