package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Ignore;
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

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Osphranter rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Marsupialia | Diprotodontia | Phalangerida | Macropodoidea | Macropodidae | Macropodinae | Osphranter | Osphranter rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | subclass | order | suborder | superfamily | family | subfamily | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:e6aff6af-ff36-4ad5-95f2-2dfdcca8caff"));
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

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Osphranter rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Marsupialia | Diprotodontia | Phalangerida | Macropodoidea | Macropodidae | Macropodinae | Osphranter | Osphranter rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | subclass | order | suborder | superfamily | family | subfamily | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:e6aff6af-ff36-4ad5-95f2-2dfdcca8caff"));
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

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Osphranter rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:e6aff6af-ff36-4ad5-95f2-2dfdcca8caff"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Marsupialia | Diprotodontia | Phalangerida | Macropodoidea | Macropodidae | Macropodinae | Osphranter | Osphranter rufus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | subclass | order | suborder | superfamily | family | subfamily | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is("Red Kangaroo @en"));
    }

    @Test
    public void lookupByNameSpermacoce() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Spermacoce");
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Spermacoce"));
        String actualExternalId = enrich.get(PropertyAndValueDictionary.EXTERNAL_ID);
        assertThat(actualExternalId, is("http://id.biodiversity.org.au/node/apni/7845073"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Plantae | Charophyta | Equisetopsida | Magnoliidae | Gentianales | Rubiaceae | Spermacoce"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | subclass | order | family | genus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("http://id.biodiversity.org.au/node/apni/9443092 | http://id.biodiversity.org.au/node/apni/9443091 | http://id.biodiversity.org.au/node/apni/9443090 | http://id.biodiversity.org.au/node/apni/9443089 | http://id.biodiversity.org.au/node/apni/9387333 | http://id.biodiversity.org.au/node/apni/8807273 | http://id.biodiversity.org.au/node/apni/7845073"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));

        assertThat(ExternalIdUtil.urlForExternalId(actualExternalId), is("http://id.biodiversity.org.au/node/apni/7845073"));
    }

    @Test
    public void lookupPretestisAustralianus() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Pretestis australianus");
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Pretestis australianus"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:127a9e96-afae-4dde-95be-52b41e8a2e58"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Platyhelminthes | Trematoda | Plagiorchiida | Pronocephalata | Paramphistomoidea | Cladorchiidae | Pretestis | Pretestis australianus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | suborder | superfamily | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    @Ignore(value = "see https://github.com/AtlasOfLivingAustralia/bie-index/issues/165")
    @Test
    public void lookupTaxonByName2() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.NAME, "Abbreviata");
        }
        };
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Abbreviata"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("AFD:39683706-f5b1-43be-934b-5fdf4f5e3150"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Nematoda | Chromadorea | Plectia | Spirurida | Spirurina | Camallanoidea | Physalopteridae | Physalopterinae | Abbreviata"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | subclass | order | suborder | superfamily | family | subfamily | genus"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    @Test
    public void lookupTaxonByInvalidName() throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        Map<String, String> enrich = new AtlasOfLivingAustraliaService().enrich(props);
        assertThat(enrich.isEmpty(), is(true));
    }

}
