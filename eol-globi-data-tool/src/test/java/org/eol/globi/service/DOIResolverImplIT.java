package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DOIResolverImplIT {

    @Test
    public void resolveDOIByReferenceNoMatch() throws IOException {
        String doi = new DOIResolverImpl().findDOIForReference("James D. Simons Food habits and trophic structure of the demersal fish assemblages on the Mississippi-Alabama continental shelf");
        assertThat(doi, is(nullValue()));
    }

    @Test
    public void resolveDOIByReferenceMatch() throws IOException {
        String doi = new DOIResolverImpl().findDOIForReference("J. N. Kremer and S. W. Nixon, A Coastal Marine Ecosystem:  Simulation and Analysis, Vol. 24 of Ecol. Studies (Springer-Verlag, Berlin, 1978), from p. 12.");
        assertThat(doi, is("http://dx.doi.org/10.1002/bimj.4710230217"));
    }

    @Test
    public void findCitationForDOI() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI("http://dx.doi.org/10.1086/283073");
        assertThat(citationForDOI, is("Menge BA, Sutherland JP. Species Diversity Gradients: Synthesis of the Roles of Predation, Competition, and Temporal Heterogeneity. The American Naturalist [Internet]. 1976 January;110(973):351. Available from: http://dx.doi.org/10.1086/283073"));
    }

    @Test
    public void findCitationForDOI2() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI("http://dx.doi.org/10.1371/journal.pone.0052967");
        assertThat(citationForDOI, is("García-Robledo C, Erickson DL, Staines CL, Erwin TL, Kress WJ. Tropical Plant–Herbivore Networks: Reconstructing Species Interactions Using DNA Barcodes Heil M, editor. PLoS ONE [Internet]. 2013 January 8;8(1):e52967. Available from: http://dx.doi.org/10.1371/journal.pone.0052967"));
    }


}
