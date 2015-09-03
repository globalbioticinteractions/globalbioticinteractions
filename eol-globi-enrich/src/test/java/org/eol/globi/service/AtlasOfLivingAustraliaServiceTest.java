package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AtlasOfLivingAustraliaServiceTest {

    @Test
    public void lookupTaxonByGUID() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "AFD:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae");
            }
        };
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Macropus rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Diprotodontia | Macropodidae | Macropus | Macropus rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is("Red Kangaroo @en"));
    }

    @Test
    public void lookupTaxonByRedirectedGUID() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "AFD:aa745ff0-c776-4d0e-851d-369ba0e6f537");
            }
        };
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(Collections.unmodifiableMap(props));

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Macropus rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Diprotodontia | Macropodidae | Macropus | Macropus rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is("Red Kangaroo @en"));
    }

    @Test
    public void lookupTaxonByInvalidGUID() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "urn:lsxxx:bla");
            }
        };
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);
        assertThat(enrich.size(), is(1));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsxxx:bla"));
    }

    @Test
    public void lookupTaxonByName() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Macropus rufus");
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Macropus rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Diprotodontia | Macropodidae | Macropus | Macropus rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is("Red Kangaroo @en"));
    }

    @Test
    public void lookupByNameSpermacoce() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Spermacoce");
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Spermacoce"));
        String actualExternalId = enrich.get(PropertyAndValueDictionary.EXTERNAL_ID);
        assertThat(actualExternalId, is("urn:lsid:biodiversity.org.au:apni.taxon:698136"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Plantae | Charophyta | Equisetopsida | Gentianales | Rubiaceae | Spermacoce | "));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("urn:lsid:biodiversity.org.au:apni.taxon:661518 | urn:lsid:biodiversity.org.au:apni.taxon:412162 | urn:lsid:biodiversity.org.au:apni.taxon:406945 | urn:lsid:biodiversity.org.au:apni.taxon:407079 | urn:lsid:biodiversity.org.au:apni.taxon:399724 | urn:lsid:biodiversity.org.au:apni.taxon:698136 | "));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));

        assertThat(ExternalIdUtil.urlForExternalId(actualExternalId), is("http://biodiversity.org.au/apni.taxon/698136"));
    }

    @Test
    public void lookupPretestisAustralianus() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Pretestis australianus");
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Pretestis australianus"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:98342a00-e3a3-4a27-be17-9d20477be54c"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Platyhelminthes | Trematoda | Plagiorchiida | Cladorchiidae | Pretestis | Pretestis australianus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    @Test
    public void lookupTaxonByName2() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.NAME, "Abbreviata");
        }
        };
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Abbreviata"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:39683706-f5b1-43be-934b-5fdf4f5e3150"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Nematoda | Chromadorea | Spirurida | Physalopteridae | Abbreviata | "));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    @Test
    public void lookupTaxonByInvalidName() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);
        assertThat(enrich.isEmpty(), is(true));
    }
}
