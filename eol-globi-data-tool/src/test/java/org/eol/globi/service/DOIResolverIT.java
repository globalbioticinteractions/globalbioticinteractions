package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DOIResolverIT {

    @Test
    public void resolveDOIByReferenceNoMatch() throws IOException {
        String doi = new DOIResolver().findDOIForReference("James D. Simons Food habits and trophic structure of the demersal fish assemblages on the Mississippi-Alabama continental shelf");
        assertThat(doi, is(nullValue()));
    }

    @Test
    public void resolveDOIByReferenceMatch() throws IOException {
        String doi = new DOIResolver().findDOIForReference("J. N. Kremer and S. W. Nixon, A Coastal Marine Ecosystem:  Simulation and Analysis, Vol. 24 of Ecol. Studies (Springer-Verlag, Berlin, 1978), from p. 12.");
        assertThat(doi, is("http://dx.doi.org/10.1002/bimj.4710230217"));
    }


}
