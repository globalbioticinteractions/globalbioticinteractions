package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class NCBIServiceTest {

    @Test
    public void lookupPathByTaxonId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:9606");
        }};
        Map<String, String> enrich = enricher.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("cellular organisms | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is(" | superkingdom |  | kingdom |  |  |  | phylum | subphylum |  |  |  |  |  |  |  |  | class |  |  |  | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is("man @en | human @en"));
    }

    @Test
    public void lookupPathByNonNumericTaxonId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:donaldduck");
        }};
        Map<String, String> enrich = enricher.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:donaldduck"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is(nullValue()));
    }

    @Test
    public void lookupPathByNonExistentTaxonId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        final String nonExistentId = "NCBI:111111111111111111111111111";
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, nonExistentId);
        }};
        Map<String, String> enrich = enricher.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nonExistentId));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is(nullValue()));
    }

    @Test
    public void lookupPathByName() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        }};
        Map<String, String> enrich = enricher.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
    }

    @Test
    public void lookupPathByPreviouslyUnmatchedId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:235106");
        }};
        Map<String, String> enrich = enricher.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Influenza A virus (A/Taiwan/0562/1995(H1N1))"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), containsString("Influenzavirus"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:235106"));
    }


}
