package org.eol.globi.service;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GulfBaseServiceTest {

    @Test
    public void lookupRankPath() throws TaxonPropertyLookupServiceException {
        OfflineService service = new GulfBaseService();
        assertThat(service.canLookupProperty(Taxon.PATH), is(true));
        assertThat(service.lookupPropertyValueByTaxonName("Haplognathia rosea", Taxon.PATH), is("Animalia Gnathostomulida Filospermoidea Haplognathiidae Haplognathia"));
        assertThat(service.lookupPropertyValueByTaxonName("Ariopsis felis", Taxon.PATH), is("Animalia Chordata Vertebrata Actinopterygii Siluriformes Ariidae Ariopsis"));
    }

    @Test
    public void lookupExternalId() throws TaxonPropertyLookupServiceException {
        OfflineService service = new GulfBaseService();
        assertThat(service.canLookupProperty(NodeBacked.EXTERNAL_ID), is(true));
        assertThat(service.lookupPropertyValueByTaxonName("Haplognathia rosea", NodeBacked.EXTERNAL_ID), is("BioGoMx:Spp-26-0003"));
        assertThat(service.lookupPropertyValueByTaxonName("Ariopsis felis", NodeBacked.EXTERNAL_ID), is("BioGoMx:Spp-75-0281"));
    }
}
