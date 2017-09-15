package org.eol.globi.taxon;

import org.junit.Test;

import java.io.IOException;

public class GulfBaseTaxonReaderFactoryTest {

    @Test
    public void allReaders() throws IOException {
        new GulfBaseTaxonReaderFactory().getAllReaders();
    }

}