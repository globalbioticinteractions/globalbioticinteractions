package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ITISServiceIntegrationTest {


    @Test
    public void lookupPathByUnacceptedTSN() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "ITIS:167353");
        }};
        Map<String, String> enrich = itisService.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:692068"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Actinopterygii | Teleostei | Acanthopterygii | Scorpaeniformes | Cottoidei | Cottoidea | Cottidae | Scorpaenichthys | Scorpaenichthys marmoratus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:161061 | ITIS:161105 | ITIS:166082 | ITIS:166702 | ITIS:167185 | ITIS:643429 | ITIS:167196 | ITIS:167352 | ITIS:692068"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Superorder | Order | Suborder | Superfamily | Family | Genus | Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Scorpaenichthys marmoratus"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
    }

    @Test
    public void lookupPathByAcceptedTSN() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "ITIS:692068");
        }};
        Map<String, String> enrich = itisService.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:692068"));
        assertPathLength(enrich, 15);
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:161061 | ITIS:161105 | ITIS:166082 | ITIS:166702 | ITIS:167185 | ITIS:643429 | ITIS:167196 | ITIS:167352 | ITIS:692068"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Actinopterygii | Teleostei | Acanthopterygii | Scorpaeniformes | Cottoidei | Cottoidea | Cottidae | Scorpaenichthys | Scorpaenichthys marmoratus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Superorder | Order | Suborder | Superfamily | Family | Genus | Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Scorpaenichthys marmoratus"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
    }

    @Test
    public void lookupPathByAcceptedTSNWithSubspecies() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "ITIS:552761");
        }};
        Map<String, String> enrich = itisService.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:552761"));
        assertPathLength(enrich, 14);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Pecari tajacu"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
    }

    private void assertPathLength(Map<String, String> enrich, int expectedPathLength) {
        assertThat(getLength(enrich, PropertyAndValueDictionary.PATH_IDS), is(expectedPathLength));
        assertThat(getLength(enrich, PropertyAndValueDictionary.PATH), is(expectedPathLength));
        assertThat(getLength(enrich, PropertyAndValueDictionary.PATH_NAMES), is(expectedPathLength));
    }

    private int getLength(Map<String, String> enrich, String pathIds) {
        return StringUtils.splitPreserveAllTokens(enrich.get(pathIds), CharsetConstant.SEPARATOR_CHAR).length;
    }

}
