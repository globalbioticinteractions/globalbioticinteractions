package org.eol.globi.util;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class ExternalIdUtilTest {

    @Test
    public void mapping() {
        assertThat(ExternalIdUtil.urlForExternalId("http://blabla"), is("http://blabla"));
        assertThat(ExternalIdUtil.urlForExternalId("https://blabla"), is("https://blabla"));
        assertThat(ExternalIdUtil.urlForExternalId("doi:10.some/DOI#12"), is("https://doi.org/10.some/DOI%2312"));
        assertThat(ExternalIdUtil.urlForExternalId("DOI:10.some/DOI#12"), is("https://doi.org/10.some/DOI%2312"));
        assertThat(ExternalIdUtil.urlForExternalId("https://doi.org/10.someDOI%2312"), is("https://doi.org/10.someDOI%2312"));
        assertThat(ExternalIdUtil.urlForExternalId("ENVO:00001995"), is("http://purl.obolibrary.org/obo/ENVO_00001995"));
        assertThat(ExternalIdUtil.urlForExternalId("bioinfo:ref:147884"), is("http://bioinfo.org.uk/html/b147884.htm"));
        assertThat(ExternalIdUtil.urlForExternalId("IF:700605"), is("http://www.indexfungorum.org/names/NamesRecord.asp?RecordID=700605"));
        assertThat(ExternalIdUtil.urlForExternalId("NCBI:7215"), is("https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=7215"));
        assertThat(ExternalIdUtil.urlForExternalId("OTT:563163"), is("https://tree.opentreeoflife.org/opentree/ottol@563163"));
        assertThat(ExternalIdUtil.urlForExternalId("GBIF:2435035"), is("http://www.gbif.org/species/2435035"));
        assertThat(ExternalIdUtil.urlForExternalId("NBN:NHMSYS0000080189"), is("https://data.nbn.org.uk/Taxa/NHMSYS0000080189"));
        assertThat(ExternalIdUtil.urlForExternalId("IRMNG:10201332"), is("http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?sp_id=10201332"));
        assertThat(ExternalIdUtil.urlForExternalId("IRMNG:1012185"), is("http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?gen_id=1012185"));
        assertThat(ExternalIdUtil.urlForExternalId("IRMNG:104889"), is("http://www.marine.csiro.au/mirrorsearch/ir_search.list_genera?fam_id=104889"));
        assertThat(ExternalIdUtil.urlForExternalId("ITIS:104889"), is("http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=104889"));
        assertThat(ExternalIdUtil.urlForExternalId("urn:lsid:biodiversity.org.au:apni.taxon:168083"), is("http://id.biodiversity.org.au/apni.taxon/168083"));
        assertThat(ExternalIdUtil.urlForExternalId("INAT:4493862"), is("https://www.inaturalist.org/observations/4493862"));
        assertThat(ExternalIdUtil.urlForExternalId("INAT_TAXON:406089"), is("https://inaturalist.org/taxa/406089"));
        assertThat(ExternalIdUtil.urlForExternalId("WD:Q140"), is("https://www.wikidata.org/wiki/Q140"));
        assertThat(ExternalIdUtil.urlForExternalId("GEONAMES:123"), is("http://www.geonames.org/123"));
        assertThat(ExternalIdUtil.urlForExternalId("ALATaxon:NZOR-4-77345"), is("https://bie.ala.org.au/species/NZOR-4-77345"));
        assertThat(ExternalIdUtil.urlForExternalId("BioGoMx:Spp-23-0494"), is("http://gulfbase.org/biogomx/biospecies.php?species=Spp-23-0494"));
        assertThat(ExternalIdUtil.urlForExternalId("EOL_V2:1234"), is("https://doi.org/10.5281/zenodo.1495266#1234"));
        assertThat(ExternalIdUtil.urlForExternalId("PLAZI:99915444-EC70-3196-7F2D-637F418F0730"), is("http://treatment.plazi.org/id/99915444-EC70-3196-7F2D-637F418F0730"));
    }

    @Test
    public void urlToPrefix() {
        assertThat(ExternalIdUtil.prefixForUrl("http://www.geonames.org/"), is(TaxonomyProvider.GEONAMES.getIdPrefix()));
    }

    @Test
    public void gbifTaxon() {
        assertThat(ExternalIdUtil.taxonomyProviderFor("https://www.gbif.org/species/5110848"), is(TaxonomyProvider.GBIF));
    }

    @Test
    public void stripIdPrefix() {
        assertThat(ExternalIdUtil.stripPrefix(TaxonomyProvider.GBIF, "https://www.gbif.org/species/5110848"), is("5110848"));
    }

    @Test
    public void itisTaxon() {
        assertThat(
                ExternalIdUtil.taxonomyProviderFor("https://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=535920"),
                is(TaxonomyProvider.ITIS)
        );
    }

    @Test
    public void ncbiTaxon() {
        assertThat(
                ExternalIdUtil.taxonomyProviderFor("NCBI:txid9606"),
                is(TaxonomyProvider.NCBI)
        );
    }

    @Test
    public void ncbiTaxonStripPrefix() {
        assertThat(
                ExternalIdUtil.stripPrefix(TaxonomyProvider.NCBI, "NCBI:txid9606"),
                is("9606")
        );
    }

    @Test
    public void ncbiTaxonStripPrefixWhitespace() {
        assertThat(
                ExternalIdUtil.stripPrefix(TaxonomyProvider.NCBI, "NCBI:txid9606 "),
                is("9606")
        );
    }

    @Test
    public void ncbiOBOTaxon() {
        assertThat(
                ExternalIdUtil.taxonomyProviderFor("http://purl.obolibrary.org/obo/NCBITaxon_9606"),
                is(TaxonomyProvider.NCBI)
        );
    }

    @Test
    public void stripITISIdPrefix() {
        assertThat(ExternalIdUtil.stripPrefix(TaxonomyProvider.ITIS, "https://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=535920"),
                is("535920"));
    }

    @Test
    public void fishBaseMapping() {
        assertThat(ExternalIdUtil.urlForExternalId("FBC:SLB:SpecCode:69195"), is("http://sealifebase.org/Summary/SpeciesSummary.php?id=69195"));
        assertThat(ExternalIdUtil.urlForExternalId("FBC:FB:SpecCode:947"), is("http://fishbase.org/summary/947"));
    }

    @Test
    public void mammalSpeciesOfTheWorldUrl() {
        assertThat(ExternalIdUtil.urlForExternalId("MSW:12100795"), is("http://www.departments.bucknell.edu/biology/resources/msw3/browse.asp?s=y&id=12100795"));
    }

    @Test
    public void mammalSpeciesOfTheWorldId() {
        String url = "http://www.departments.bucknell.edu/biology/resources/msw3/browse.asp?s=y&id=12100795";
        assertThat(ExternalIdUtil.taxonomyProviderFor(url), is(TaxonomyProvider.MSW));
        assertThat(ExternalIdUtil.stripPrefix(TaxonomyProvider.MSW, url), is("12100795"));
    }

    @Test
    public void getExternalId() {
        assertThat(ExternalIdUtil.getUrlFromExternalId("{ \"data\": [[]]}"), is("{}"));
        assertThat(ExternalIdUtil.getUrlFromExternalId("{ \"data\": []}"), is("{}"));
        assertThat(ExternalIdUtil.getUrlFromExternalId("{}"), is("{}"));
    }

    @Test
    public void buildCitation() {
        assertThat(ExternalIdUtil.toCitation("Joe Smith", "my study", "1984"), is("Joe Smith. 1984. my study"));
        assertThat(ExternalIdUtil.toCitation("Joe Smith", null, "1984"), is("Joe Smith. 1984"));
        assertThat(ExternalIdUtil.toCitation("Joe Smith", "my study", null), is("Joe Smith. my study"));
    }

    @Test
    public void isLikelyId() {
        assertFalse(ExternalIdUtil.isLikelyId("1234"));
        assertFalse(ExternalIdUtil.isLikelyId("something here"));
        assertFalse(ExternalIdUtil.isLikelyId(null));
        assertTrue(ExternalIdUtil.isLikelyId("1234-123"));
        assertTrue(ExternalIdUtil.isLikelyId("bla:1234-123"));
        assertTrue(ExternalIdUtil.isLikelyId("bla:1234-123"));
        assertTrue(ExternalIdUtil.isLikelyId("https://some/thing"));
    }

    @Test
    public void isUnsupportedId() {
        assertThat(ExternalIdUtil.isSupported("urn:catalog:AMNH:Mammals:M-39582"), is(false));
    }

    @Test
    public void isSupportedId() {
        assertThat(ExternalIdUtil.isSupported("EOL:123"), is(true));
    }
}
