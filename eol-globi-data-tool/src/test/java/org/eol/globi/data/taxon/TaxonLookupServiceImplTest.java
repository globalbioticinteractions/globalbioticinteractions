package org.eol.globi.data.taxon;

import org.apache.lucene.store.RAMDirectory;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
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
        listener.addTerm(new TaxonImpl("Homo sapiens", "1234"));
        listener.addTerm(new TaxonImpl("Prefix Homo sapiens suffix", "12346"));
        listener.finish();

        Taxon[] ids = taxonLookupServiceImpl.lookupTermsByName("Homo sapiens");

        assertThat(ids.length, Is.is(1));
        assertThat(ids[0].getExternalId(), Is.is("1234"));

        taxonLookupServiceImpl.destroy();
    }

    @Test
    public void createIndexDoLookupBlankName() throws IOException {
        TaxonLookupServiceImpl taxonLookupServiceImpl = new TaxonLookupServiceImpl(new RAMDirectory());

        taxonLookupServiceImpl.start();
        taxonLookupServiceImpl.addTerm(new TaxonImpl("Homo sapiens", "1234"));
        taxonLookupServiceImpl.addTerm(new TaxonImpl("Prefix Homo sapiens suffix", "12346"));
        taxonLookupServiceImpl.finish();

        Taxon[] ids = taxonLookupServiceImpl.lookupTermsByName(null);

        assertThat(ids.length, Is.is(0));

        taxonLookupServiceImpl.destroy();
    }

}
