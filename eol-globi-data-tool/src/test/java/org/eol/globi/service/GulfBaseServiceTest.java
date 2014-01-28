package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GulfBaseServiceTest {

    @Test
    public void lookupRankPath() throws TaxonPropertyLookupServiceException {
        OfflineService service = new GulfBaseService();
        assertThat(service.lookupPropertyValueByTaxonName("Haplognathia rosea", PropertyAndValueDictionary.PATH), is("Animalia Gnathostomulida Filospermoidea Haplognathiidae Haplognathia"));
        assertThat(service.lookupPropertyValueByTaxonName("Ariopsis felis", PropertyAndValueDictionary.PATH), is("Animalia Chordata Vertebrata Actinopterygii Siluriformes Ariidae Ariopsis"));
    }

    @Test
    public void lookupExternalId() throws TaxonPropertyLookupServiceException {
        OfflineService service = new GulfBaseService();
        assertThat(service.lookupPropertyValueByTaxonName("Haplognathia rosea", PropertyAndValueDictionary.EXTERNAL_ID), is("BioGoMx:Spp-26-0003"));
        assertThat(service.lookupPropertyValueByTaxonName("Ariopsis felis", PropertyAndValueDictionary.EXTERNAL_ID), is("BioGoMx:Spp-75-0281"));
    }
}
