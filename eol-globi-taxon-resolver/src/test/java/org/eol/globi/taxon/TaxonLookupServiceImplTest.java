package org.eol.globi.taxon;

import org.apache.lucene.store.RAMDirectory;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TaxonLookupServiceImplTest {

    @Test
    public void createIndexDoLookup() throws IOException {
        lookup(new RAMDirectory());
    }

    @Test
    public void createIndexDoLookupFile() throws IOException {
        lookup(null);
    }

    public void lookup(RAMDirectory indexDir) throws IOException {
        TaxonLookupServiceImpl service = new TaxonLookupServiceImpl(indexDir);

        service.start();
        TaxonImpl term = new TaxonImpl("Homo sapiens", "1234");
        term.setPath("one | two | three");
        term.setPathIds("1 | 2 | 3");
        term.setPathNames("name1 | name2 | name3");
        term.setCommonNames("Mensch");
        service.addTerm(term);
        TaxonImpl term1 = new TaxonImpl("Prefix Homo sapiens suffix", "12346");
        term1.setPathIds("1 | 2 | 3");
        service.addTerm(term1);
        service.finish();

        Taxon[] ids = service.lookupTermsByName("Homo sapiens");

        assertThat(ids.length, Is.is(1));
        assertThat(ids[0].getExternalId(), Is.is("1234"));

        ids = service.lookupTermsById("1234");

        assertThat(ids.length, Is.is(1));
        assertThat(ids[0].getExternalId(), Is.is("1234"));
        assertThat(ids[0].getName(), Is.is("Homo sapiens"));
        assertThat(ids[0].getCommonNames(), Is.is("Mensch"));
        assertThat(ids[0].getPathIds(), Is.is("1 | 2 | 3"));
        assertThat(ids[0].getPath(), Is.is("one | two | three"));
        assertThat(ids[0].getPathNames(), Is.is("name1 | name2 | name3"));

        assertThat(service.lookupTermsById("12346")[0].getPathIds(), Is.is("1 | 2 | 3"));

        service.destroy();
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
