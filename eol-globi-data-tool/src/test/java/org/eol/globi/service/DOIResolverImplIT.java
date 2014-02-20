package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
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
        assertThat(citationForDOI, is("Menge BA, Sutherland JP. Species Diversity Gradients: Synthesis of the Roles of Predation, Competition, and Temporal Heterogeneity. American Naturalist, The [Internet]. 1976 January;110(973):351. Available from: http://dx.doi.org/10.1086/283073"));
    }

    @Test
    public void findCitationForDOIEscaped() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI("http://dx.doi.org/10.1577/1548-8659(1973)102<511:fhojmf>2.0.co;2");
        assertThat(citationForDOI, is("Carr WES, Adams CA. Food Habits of Juvenile Marine Fishes Occupying Seagrass Beds in the Estuarine Zone near Crystal River, Florida. Transactions of the American Fisheries Society [Internet]. 1973 July;102(3):511-540. Available from: http://dx.doi.org/10.1577/1548-8659(1973)102<511:FHOJMF>2.0.CO;2"));
    }

    @Test
    public void findCitationForDOI2() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI("http://dx.doi.org/10.1371/journal.pone.0052967");
        assertThat(citationForDOI, is("García-Robledo C, Erickson DL, Staines CL, Erwin TL, Kress WJ. Tropical Plant–Herbivore Networks: Reconstructing Species Interactions Using DNA Barcodes Heil M, editor. PLoS ONE [Internet]. 2013 January 8;8(1):e52967. Available from: http://dx.doi.org/10.1371/journal.pone.0052967"));
    }
@Test
    public void findCitationForDOI3() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI("http://dx.doi.org/10.2307/177149");
        assertThat(citationForDOI, is("Yodzis P. DIFFUSE EFFECTS IN FOOD WEBS. Ecology [Internet]. 2000 January;81(1):261-266. Available from: http://dx.doi.org/10.1890/0012-9658(2000)081[0261:DEIFW]2.0.CO;2"));
    }

    @Test
    public void findMalformedCitationWithMalformedDOIURL() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI("this ain't no uRL");
        assertThat(citationForDOI, nullValue());
    }

    @Test
    public void findCitationForDOIWithUnescapeChars() throws IOException, URISyntaxException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI("http://dx.doi.org/10.1642/0004-8038(2005)122[1182:baemfa]2.0.co;2");
        assertThat(citationForDOI, is(notNullValue()));
    }

}
