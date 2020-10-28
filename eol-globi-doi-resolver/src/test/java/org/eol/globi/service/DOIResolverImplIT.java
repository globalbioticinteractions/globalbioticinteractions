package org.eol.globi.service;

import org.globalbioticinteractions.doi.DOI;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
public class DOIResolverImplIT {

    private static final String HOCKING = "Hocking, B. 1968. Insect-flower associations in the high Arctic with special reference to nectar. Oikos 19:359-388.";
    private static final String MEDAN = "Medan, D., N. H. Montaldo, M. Devoto, A. Mantese, V. Vasellati, and N. H. Bartoloni. 2002. Plant-pollinator relationships at two altitudes in the Andes of Mendoza, Argentina. Arctic Antarctic and Alpine Research 34:233-241.";
    private static final DOI HOCKING_DOI = new DOI("2307", "3565022");
    private static final String MEDAN_DOI = "10.2307/1552480";

    @Test
    public void resolveDOIByReferenceNoMatch() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("James D. Simons Food habits and trophic structure of the demersal fish assemblages on the Mississippi-Alabama continental shelf");
        assertThat(doi, is(nullValue()));
    }

    @Test
    public void resolveDOIByReferenceNoMatch2() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("Petanidou, T. (1991). Pollination ecology in a phryganic ecosystem. Unp. PhD. Thesis, Aristotelian University, Thessaloniki.");
        assertThat(doi, is(nullValue()));
    }
    @Test
    public void resolveDOIByReferenceMatch8() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("Tutin, C.E.G., Ham, R.M., White, L.J.T., Harrison, M.J.S. 1997. The primate community of the Lopé Reserve, Gabon: diets, responses to fruit scarcity, and effects on biomass. American Journal of Primatology, 42: 1-24.");
        assertThat(doi.toString(), is("10.1002/(sici)1098-2345(1997)42:1<1::aid-ajp1>3.0.co;2-0"));
    }

    @Test
    public void resolveDOIByReferenceNoMatchToBookReview() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("J. N. Kremer and S. W. Nixon, A Coastal Marine Ecosystem:  Simulation and Analysis, Vol. 24 of Ecol. Studies (Springer-Verlag, Berlin, 1978), from p. 12.");
        assertThat(doi, is(nullValue()));
    }

    @Test
    public void resolveDOIByNonExistentCitation() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("A. Thessen. 2014. Species associations extracted from EOL text data objects via text mining. Accessed at <associations_all_revised.txt> on 05 Feb 2018 and add some more and other things");
        assertThat(doi, is(nullValue()));
    }

    @Test
    public void resolveDOIByNonExistentCitation3() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("Bundgaard, M. (2003). Tidslig og rumlig variation i et plante-bestøvernetværk. Msc thesis. University of Aarhus. Aarhus, Denmark.");
        assertThat(doi, is(nullValue()));
    }

    @Test
    public void resolveDOIByNonExistentCitation2() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("donald duck and mickey mouse run around");
        assertThat(doi, is(nullValue()));
    }

    @Test
    public void possiblyMalformedDOI() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("Tutin, C.E.G., Ham, R.M., White, L.J.T., Harrison, M.J.S. 1997. The primate community of the Lopé Reserve, Gabon: diets, responses to fruit scarcity, and effects on biomass. American Journal of Primatology, 42: 1-24.");
        assertNotNull(doi);
        assertThat(doi.toString(), is("10.1002/(sici)1098-2345(1997)42:1<1::aid-ajp1>3.0.co;2-0"));
    }

    @Test
    @Ignore
    public void resolveDOIByReferenceURL() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("http://www.ncbi.nlm.nih.gov/nuccore/7109271");
        assertThat(doi, is("10.1002/bimj.4710230217"));
    }

    @Test
    public void resolveDOIByReferenceTamarins() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("Raboy, Becky E., and James M. Dietz. Diet, Foraging, and Use of Space in Wild Golden-headed Lion Tamarins. American Journal of Primatology, 63(1):, 2004, 1-15. Accessed April 20, 2015. http://hdl.handle.net/10088/4251.");
        assertThat(doi.toString(), is("10.1002/ajp.20032"));
    }

    @Test
    public void resolveDOIByReferenceMatch2() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor(MEDAN);
        assertThat(doi.toString(), is(MEDAN_DOI));
    }

    @Test
    public void resolveDOIByReferenceMatch3() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor(HOCKING);
        assertThat(doi.toString(), is(HOCKING_DOI.toString()));
    }

    @Test
    public void resolveDOIByReferenceMatchBatch() throws IOException {
        Map<String, DOI> doiMap = new DOIResolverImpl().resolveDoiFor(Arrays.asList(MEDAN, HOCKING));
        assertThat(doiMap.get(HOCKING).toString(), is(HOCKING_DOI.toString()));
        assertThat(doiMap.get(MEDAN).toString(), is(MEDAN_DOI));
    }


    @Test
    public void resolveDOIBugFixedServerError() throws IOException {
        String citation = new DOIResolverImpl().findCitationForDOI(new DOI("2307", "4070736"));
        assertThat(citation, is("Anon. McAtee’s “Food Habits of the Grosbeaks” Food Habits of the Grosbeaks W. L. McAtee. The Auk [Internet]. 1908 April;25(2):245–246. Available from: http://dx.doi.org/10.2307/4070736"));
    }

    @Test
    public void resolveWithCrossRefIntermittentRuntimeError() throws IOException {
        // see https://github.com/CrossRef/rest-api-doc/issues/93
        String citation = new DOIResolverImpl().findCitationForDOI(new DOI("1017", "s0266467499000760"));
        assertThat(citation, is("Poulin B, Wright SJ, Lefebvre G, Calderón O. Interspecific synchrony and asynchrony in the fruiting phenologies of congeneric bird-dispersed plants in Panama. Journal of Tropical Ecology [Internet]. 1999 March;15(2):213–227. Available from: http://dx.doi.org/10.1017/s0266467499000760"));
    }

    @Test
    public void resolveBioInfoCitation() throws IOException {
        DOI doi = new DOIResolverImpl().resolveDoiFor("Galea, V.J. & Price, T.V.. 1988. Infection of Lettuce by Microdochium panattonianum. Transactions of the British Mycological Society. Vol Vol 91 (3). pp 419-425");
        DOI expectedDoi = new DOI("1016", "s0007-1536(88)80117-7");
        assertThat(doi, is(expectedDoi));
        String citation = new DOIResolverImpl().findCitationForDOI(doi);
        assertThat(citation, containsString("Galea VJ, Price TV"));
    }

    @Test
    public void findCitationForDOIStrangeCharacters() throws IOException {
        String citation = new DOIResolverImpl().findCitationForDOI(new DOI("1007", "s00300-004-0645-x"));
        assertThat(citation, is("La Mesa M, Dalú M, Vacchi M. Trophic ecology of the emerald notothen Trematomus bernacchii (Pisces, Nototheniidae) from Terra Nova Bay, Ross Sea, Antarctica. Polar Biology [Internet]. 2004 July 27;27(11):721–728. Available from: http://dx.doi.org/10.1007/s00300-004-0645-x"));
    }

    @Test
    public void findCitationForDOI() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(new DOI("1086", "283073"));
        assertThat(citationForDOI, is("Menge BA, Sutherland JP. Species Diversity Gradients: Synthesis of the Roles of Predation, Competition, and Temporal Heterogeneity. The American Naturalist [Internet]. 1976 May;110(973):351–369. Available from: http://dx.doi.org/10.1086/283073"));
    }

    @Test
    public void findCitationForDOITRex() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(new DOI("1073", "pnas.1216534110"));
        assertThat(citationForDOI, is("DePalma RA, Burnham DA, Martin LD, Rothschild BM, Larson PL. Physical evidence of predatory behavior in Tyrannosaurus rex. Proceedings of the National Academy of Sciences [Internet]. 2013 July 15;110(31):12560–12564. Available from: http://dx.doi.org/10.1073/pnas.1216534110"));
    }

    @Test
    public void findCitationForDOIEscaped() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(new DOI("1577", "1548-8659(1973)102<511:fhojmf>2.0.co;2"));
        assertThat(citationForDOI, is("Carr WES, Adams CA. Food Habits of Juvenile Marine Fishes Occupying Seagrass Beds in the Estuarine Zone near Crystal River, Florida. Transactions of the American Fisheries Society [Internet]. 1973 July;102(3):511–540. Available from: http://dx.doi.org/10.1577/1548-8659(1973)102<511:fhojmf>2.0.co;2"));
    }

    @Test
    public void findCitationForDOI2() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(new DOI("1371", "journal.pone.0052967"));
        assertThat(citationForDOI, is("García-Robledo C, Erickson DL, Staines CL, Erwin TL, Kress WJ. Tropical Plant–Herbivore Networks: Reconstructing Species Interactions Using DNA Barcodes Heil M, editor. PLoS ONE [Internet]. 2013 January 8;8(1):e52967. Available from: http://dx.doi.org/10.1371/journal.pone.0052967"));
    }

    @Test
    public void findCitationForDOI3() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(new DOI("2307", "177149"));
        assertThat(citationForDOI, is("Yodzis P. DIFFUSE EFFECTS IN FOOD WEBS. Ecology [Internet]. 2000 January;81(1):261–266. Available from: http://dx.doi.org/10.1890/0012-9658(2000)081[0261:DEIFW]2.0.CO;2"));
    }

    @Test
    public void findMalformedCitationWithMalformedDOIURL() throws IOException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(null);
        assertThat(citationForDOI, nullValue());
    }

    @Test(expected = IOException.class)
    public void findNotResponding() throws IOException {
        new DOIResolverImpl("http://google.com").resolveDoiFor("some reference");
    }

    @Test
    public void findCitationForDOIWithUnescapeChars() throws IOException, URISyntaxException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(new DOI("1642", "0004-8038(2005)122[1182:baemfa]2.0.co;2"));
        assertThat(citationForDOI, is(notNullValue()));
    }

    @Test
    public void findCitationForDOIVariableInternalServerError() throws IOException, URISyntaxException {
        // see https://github.com/CrossRef/rest-api-doc/issues/93
        assertThat(new DOIResolverImpl().findCitationForDOI(new DOI("1007", "s003000050412")), is(notNullValue()));
        assertThat(new DOIResolverImpl().findCitationForDOI(new DOI("1007", "s003000050412")), is(notNullValue()));
    }

    @Test
    public void findCitationForShortDOI() throws IOException, URISyntaxException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(new DOI("1007", "s13127-011-0039-1"));
        assertThat(citationForDOI, is(notNullValue()));
    }

    @Test
    public void findCitationForShortDOIUpperCase() throws IOException, URISyntaxException {
        String citationForDOI = new DOIResolverImpl().findCitationForDOI(new DOI("5962", "bhl.title.2633"));
        assertThat(citationForDOI, is(notNullValue()));
    }

}
