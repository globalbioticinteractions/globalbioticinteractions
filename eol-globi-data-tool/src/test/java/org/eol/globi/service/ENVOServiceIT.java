package org.eol.globi.service;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ENVOServiceIT {

    @Test
    public void lookupTerm() throws ENVOServiceException {
        ENVOService service = new ENVOServiceImpl();
        List<ENVOTerm> envoTerms = service.lookupBySPIREHabitat("Dung");
        assertThat(envoTerms.size(), is(1));
        ENVOTerm envoTerm = envoTerms.get(0);
        assertThat(envoTerm.getName(), is("feces"));
        assertThat(envoTerm.getId(), is("ENVO:00002003"));

        envoTerms = service.lookupBySPIREHabitat("Savannah_or_grassland");
        assertThat(envoTerms.size(), is(2));
        envoTerm = envoTerms.get(0);
        assertThat(envoTerm.getName(), is("grassland biome"));
        assertThat(envoTerm.getId(), is("ENVO:01000177"));

        envoTerm = envoTerms.get(1);
        assertThat(envoTerm.getName(), is("savanna biome"));
        assertThat(envoTerm.getId(), is("ENVO:01000178"));

        envoTerms = service.lookupBySPIREHabitat("this ain't no SPIRE habitat");
        assertThat(envoTerms.size(), is(0));

    }

}
