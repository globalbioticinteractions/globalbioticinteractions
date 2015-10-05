package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GulfBaseServiceTest {

    private static GulfBaseService gulfBaseService = null;

    @BeforeClass
    public static void init() {
        gulfBaseService = new GulfBaseService();
    }

    @Test
    public void enrichProperties() throws PropertyEnricherException {
        HashMap<String, String> m = new HashMap<String, String>();
        m.put(PropertyAndValueDictionary.NAME, "Ariopsis felis");
        Map<String, String> properties = Collections.unmodifiableMap(m);
        Map<String, String> enrich = gulfBaseService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Vertebrata | Actinopterygii | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | subphylum | class | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("BioGoMx:Spp-75-0281"));
        assertThat(ExternalIdUtil.urlForExternalId("BioGoMx:Spp-75-0281"), is("http://gulfbase.org/biogomx/biospecies.php?species=Spp-75-0281"));
    }

    @Test
    public void lookupRankPath() throws PropertyEnricherException {
        GulfBaseService gulfBaseService = new GulfBaseService();
        HashMap<String, String> m = new HashMap<String, String>();
        m.put(PropertyAndValueDictionary.NAME, "Haplognathia rosea");
        Map<String, String> properties = Collections.unmodifiableMap(m);
        Map<String, String> enrich = gulfBaseService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Haplognathia rosea"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Gnathostomulida | Filospermoidea | Haplognathiidae | Haplognathia | Haplognathia rosea"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("BioGoMx:Spp-26-0003"));
    }

    @Test
    public void lookupById() throws PropertyEnricherException {
        GulfBaseService gulfBaseService = new GulfBaseService();
        HashMap<String, String> m = new HashMap<String, String>();
        m.put(PropertyAndValueDictionary.EXTERNAL_ID, "BioGoMx:Spp-26-0003");
        Map<String, String> properties = Collections.unmodifiableMap(m);
        Map<String, String> enrich = gulfBaseService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Haplognathia rosea"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Gnathostomulida | Filospermoidea | Haplognathiidae | Haplognathia | Haplognathia rosea"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("BioGoMx:Spp-26-0003"));
    }

}
