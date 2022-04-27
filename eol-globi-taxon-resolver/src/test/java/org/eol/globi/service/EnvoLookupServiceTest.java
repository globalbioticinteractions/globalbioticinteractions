package org.eol.globi.service;

import org.eol.globi.domain.Term;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class EnvoLookupServiceTest {

    @Test
    public void lookupTerm() throws TermLookupServiceException {
        TermLookupService service = new EnvoLookupService(new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, is -> is);
            }
        });
        List<Term> terms = service.lookupTermByName("Dung");
        assertThat(terms.size(), is(1));
        Term term = terms.get(0);
        assertThat(term.getName(), is("feces"));
        assertThat(term.getId(), is("ENVO:00002003"));

        terms = service.lookupTermByName("Savannah_or_grassland");
        assertThat(terms.size(), is(2));
        term = terms.get(0);
        assertThat(term.getName(), is("grassland biome"));
        assertThat(term.getId(), is("ENVO:01000177"));

        term = terms.get(1);
        assertThat(term.getName(), is("savanna biome"));
        assertThat(term.getId(), is("ENVO:01000178"));

        terms = service.lookupTermByName("this ain't no SPIRE habitat");
        assertThat(terms.size(), is(1));

        term = terms.get(0);
        assertThat(term.getName(), is("this ain't no SPIRE habitat"));
        assertThat(term.getId(), is("no:match"));


    }

    @Test
    public void CMECShabitats() throws TermLookupServiceException {
        TermLookupService service = new EnvoLookupService(new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, is -> is);
            }
        });
        List<Term> terms = service.lookupTermByName("Marine Nearshore Subtidal");
        assertThat(terms.size(), is(1));

        Term term = terms.get(0);
        assertThat(term.getName(), is("Marine Nearshore Subtidal"));
        assertThat(term.getId(), is("no:match"));
    }

}
