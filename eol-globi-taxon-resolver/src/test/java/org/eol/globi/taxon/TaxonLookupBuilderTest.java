package org.eol.globi.taxon;

import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class TaxonLookupBuilderTest {

    @Test
    public void createIndexDoLookup() throws IOException {
        lookup(new RAMDirectory());
    }

    public void lookup(RAMDirectory indexDir) throws IOException {
        TaxonLookupBuilder service = new TaxonLookupBuilder(indexDir);

        service.start();
        TaxonImpl term = new TaxonImpl("Homo sapiens", "1234");
        term.setRank("species");
        term.setPath("one | two | three");
        term.setPathIds("1 | 2 | 3");
        term.setPathNames("name1 | name2 | name3");
        term.setCommonNames("Mensch");
        service.addTerm(term);
        TaxonImpl term1 = new TaxonImpl("Prefix Homo sapiens suffix", "12346");
        term1.setPathIds("1 | 2 | 3");
        service.addTerm(term1);
        service.finish();

        TaxonLookupServiceImpl lookup = new TaxonLookupServiceImpl(indexDir);

        Taxon[] ids = lookup.lookupTermsByName("Homo sapiens");

        assertThat(ids.length, Is.is(1));
        assertThat(ids[0].getExternalId(), Is.is("1234"));

        ids = lookup.lookupTermsById("1234");

        assertThat(ids.length, Is.is(1));
        assertThat(ids[0].getExternalId(), Is.is("1234"));
        assertThat(ids[0].getName(), Is.is("Homo sapiens"));
        assertThat(ids[0].getRank(), Is.is("species"));
        assertThat(ids[0].getCommonNames(), Is.is("Mensch"));
        assertThat(ids[0].getPathIds(), Is.is("1 | 2 | 3"));
        assertThat(ids[0].getPath(), Is.is("one | two | three"));
        assertThat(ids[0].getPathNames(), Is.is("name1 | name2 | name3"));

        assertThat(lookup.lookupTermsById("12346")[0].getPathIds(), Is.is("1 | 2 | 3"));

        service.close();
    }

    @Test
    public void createIndexDoLookupBlankName() throws IOException {
        RAMDirectory indexDir = new RAMDirectory();
        TaxonLookupBuilder taxonLookupBuilder = new TaxonLookupBuilder(indexDir);

        taxonLookupBuilder.start();
        taxonLookupBuilder.addTerm(new TaxonImpl("Homo sapiens", "1234"));
        taxonLookupBuilder.addTerm(new TaxonImpl("Prefix Homo sapiens suffix", "12346"));
        taxonLookupBuilder.finish();

        TaxonLookupServiceImpl lookup = new TaxonLookupServiceImpl(indexDir);

        Taxon[] ids = lookup.lookupTermsByName(null);

        assertThat(ids.length, Is.is(0));

    }

    @Test(expected = RuntimeException.class)
    public void writeLock() throws IOException {
        RAMDirectory indexDir = new RAMDirectory();
        TaxonLookupBuilder taxonLookupBuilder1 = new TaxonLookupBuilder(indexDir);
        TaxonLookupBuilder taxonLookupBuilder2 = new TaxonLookupBuilder(indexDir);

        try {
            taxonLookupBuilder1.start();
            taxonLookupBuilder1.addTerm(new TaxonImpl("Homo sapiens", "1234"));

            taxonLookupBuilder2.start();
            taxonLookupBuilder2.addTerm(new TaxonImpl("Prefix Homo sapiens suffix", "12346"));
        } catch (RuntimeException ex) {
            assertThat(ex.getCause(), is(instanceOf(LockObtainFailedException.class)));
            throw ex;
        }

    }

    @Test
    public void noWriteLock() throws IOException {
        RAMDirectory indexDir = new RAMDirectory();
        TaxonLookupBuilder taxonLookupBuilder1 = new TaxonLookupBuilder(indexDir);
        TaxonLookupBuilder taxonLookupBuilder2 = new TaxonLookupBuilder(indexDir);

        taxonLookupBuilder1.start();
        taxonLookupBuilder1.addTerm(new TaxonImpl("Homo sapiens", "1234"));
        taxonLookupBuilder1.finish();

        taxonLookupBuilder2.start();
        taxonLookupBuilder2.addTerm(new TaxonImpl("Prefix Homo sapiens suffix", "12346"));
        taxonLookupBuilder1.finish();

    }


}
