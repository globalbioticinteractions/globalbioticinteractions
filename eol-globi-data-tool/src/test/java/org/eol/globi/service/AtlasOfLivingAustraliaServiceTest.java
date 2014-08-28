package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AtlasOfLivingAustraliaServiceTest {

    @Test
    public void lookupTaxonByGUID() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae");
            }
        };
        new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(props.get(PropertyAndValueDictionary.NAME), is("Macropus rufus"));
        assertThat(props.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Diprotodontia | Macropodidae | Macropus | Macropus rufus"));
        assertThat(props.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae"));
        assertThat(props.get(PropertyAndValueDictionary.COMMON_NAMES), is("Red Kangaroo @en"));
    }

    @Test
    public void lookupTaxonByRedirectedGUID() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537");
            }
        };
        new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(props.get(PropertyAndValueDictionary.NAME), is("Macropus rufus"));
        assertThat(props.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Diprotodontia | Macropodidae | Macropus | Macropus rufus"));
        assertThat(props.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae"));
        assertThat(props.get(PropertyAndValueDictionary.COMMON_NAMES), is("Red Kangaroo @en"));
    }

    @Test
    public void lookupTaxonByInvalidGUID() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "urn:lsxxx:bla");
            }
        };
        new AtlasOfLivingAustraliaService().enrich(props);
        assertThat(props.size(), is(1));
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsxxx:bla"));
    }

    @Test
    public void lookupTaxonByName() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Macropus rufus");
        new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(props.get(PropertyAndValueDictionary.NAME), is("Macropus rufus"));
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae"));
        assertThat(props.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Diprotodontia | Macropodidae | Macropus | Macropus rufus"));
        assertThat(props.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(props.get(PropertyAndValueDictionary.COMMON_NAMES), is("Red Kangaroo @en"));
    }

    @Test
    public void lookupTaxonByName2() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.NAME, "Abbreviata");
        }
        };
        new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(props.get(PropertyAndValueDictionary.NAME), is("Abbreviata"));
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsid:biodiversity.org.au:afd.taxon:39683706-f5b1-43be-934b-5fdf4f5e3150"));
        assertThat(props.get(PropertyAndValueDictionary.PATH), is("Animalia | Nematoda | Chromadorea | Spirurida | Physalopteridae | Abbreviata | "));
        assertThat(props.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(props.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    @Test
    public void lookupTaxonByInvalidName() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        new AtlasOfLivingAustraliaService().enrich(props);
        assertThat(props.isEmpty(), is(true));
    }
}
