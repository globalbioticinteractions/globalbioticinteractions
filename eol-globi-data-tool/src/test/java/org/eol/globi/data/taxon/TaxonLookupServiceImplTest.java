package org.eol.globi.data.taxon;

import org.apache.lucene.store.RAMDirectory;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertThat;

public class TaxonLookupServiceImplTest {


    @Test
    public void createIndexDoLookup() throws IOException {
        TaxonLookupServiceImpl taxonLookupServiceImpl = new TaxonLookupServiceImpl(new RAMDirectory());

        TaxonImportListener listener = taxonLookupServiceImpl;
        listener.start();
        listener.addTerm(new TaxonTerm("Homo sapiens", "1234"));
        listener.addTerm(new TaxonTerm("Prefix Homo sapiens suffix", "12346"));
        listener.finish();

        String[] ids = taxonLookupServiceImpl.lookupTermIds("Homo sapiens");

        assertThat(ids.length, Is.is(1));
        assertThat(ids[0], Is.is("1234"));

        taxonLookupServiceImpl.destroy();
    }

}
