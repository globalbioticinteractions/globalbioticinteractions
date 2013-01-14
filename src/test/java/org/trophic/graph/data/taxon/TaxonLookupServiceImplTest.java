package org.trophic.graph.data.taxon;

import org.apache.lucene.store.RAMDirectory;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertThat;

public class TaxonLookupServiceImplTest {


    @Test
    public void createIndexDoLookup() throws IOException {
        TaxonLookupServiceImpl taxonLookupServiceImpl = new TaxonLookupServiceImpl(new RAMDirectory());

        TaxonImportListener listener = taxonLookupServiceImpl;
        listener.start();
        listener.addTerm("Homo sapiens", 1234L);
        listener.addTerm("Prefix Homo sapiens suffix", 12346L);
        listener.finish();

        long[] ids = taxonLookupServiceImpl.lookupTerms("Homo sapiens");

        assertThat(ids.length, Is.is(1));
        assertThat(ids[0], Is.is(1234L));

        taxonLookupServiceImpl.destroy();
    }

}
