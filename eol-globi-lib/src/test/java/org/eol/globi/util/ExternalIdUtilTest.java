package org.eol.globi.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExternalIdUtilTest {

    @Test
    public void mapping() {
        assertThat(ExternalIdUtil.urlForExternalId("http://blabla"), is("http://blabla"));
        assertThat(ExternalIdUtil.urlForExternalId("https://blabla"), is("https://blabla"));
        assertThat(ExternalIdUtil.urlForExternalId("doi:someDOI"), is("https://doi.org/someDOI"));
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
    }

    @Test
    public void fishBaseMapping() {
        assertThat(ExternalIdUtil.urlForExternalId("FBC:SLB:SpecCode:69195"), is("http://sealifebase.org/Summary/SpeciesSummary.php?id=69195"));
        assertThat(ExternalIdUtil.urlForExternalId("FBC:FB:SpecCode:947"), is("http://fishbase.org/summary/947"));
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
}
