package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ITISServiceTest {


    @Test
    public void lookupPathByUnacceptedTSN() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "ITIS:167353");
        }};
        Map<String, String> enrich = itisService.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is ("ITIS:692068"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is ("Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Osteichthyes | Actinopterygii | Neopterygii | Teleostei | Acanthopterygii | Scorpaeniformes | Cottoidei | Cottoidea | Cottidae | Scorpaenichthys | Scorpaenichthys marmoratus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is ("ITIS:692068 | ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:161030 | ITIS:161061 | ITIS:553120 | ITIS:161105 | ITIS:166082 | ITIS:166702 | ITIS:167185 | ITIS:643429 | ITIS:167196 | ITIS:167352 | ITIS:692068"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is ("Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Superorder | Order | Suborder | Superfamily | Family | Genus | Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is ("Scorpaenichthys marmoratus"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is ("Species"));
    }

    @Test
    public void lookupPathByAcceptedTSN() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "ITIS:692068");
        }};
        Map<String, String> enrich = itisService.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is ("ITIS:692068"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is ("Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Osteichthyes | Actinopterygii | Neopterygii | Teleostei | Acanthopterygii | Scorpaeniformes | Cottoidei | Cottoidea | Cottidae | Scorpaenichthys | Scorpaenichthys marmoratus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is ("ITIS:692068 | ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:161030 | ITIS:161061 | ITIS:553120 | ITIS:161105 | ITIS:166082 | ITIS:166702 | ITIS:167185 | ITIS:643429 | ITIS:167196 | ITIS:167352 | ITIS:692068"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is ("Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Superorder | Order | Suborder | Superfamily | Family | Genus | Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is ("Scorpaenichthys marmoratus"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is ("Species"));
    }


}
