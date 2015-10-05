package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.GlobalNamesService;
import org.eol.globi.taxon.GlobalNamesSources;
import org.eol.globi.taxon.TermMatchListener;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class GlobalNamesServiceTest {

    @Test
    public void createTaxaListFromNameList() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        service.findTermsForNames(Arrays.asList("1|Homo sapiens", "2|Ariopsis felis"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon, boolean isExactMatch) {
                assertNotNull(id);
                foundTaxa.add(taxon);
            }
        }, Arrays.asList(GlobalNamesSources.ITIS));

        assertThat(foundTaxa.size(), is(2));
    }


    @Test
    public void lookupITIS() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        Map<String, String> props = assertHomoSapiens(service);
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:180092"));
    }

    @Test
    public void lookupITISSynonymFails() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Corizidae");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:108477"));
    }

    @Test
    public void lookupITISSynonymSuccess() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Arius felis");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:680665"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
    }

    @Test
    public void lookupNCBI() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.NCBI);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("superkingdom | kingdom | phylum | subphylum | class | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    @Test
    public void lookupWoRMS() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Ariopsis felis");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Actinopterygii | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is(" |  |  |  |  |  | WORMS:158709"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("WORMS:158709"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), not(containsString("hardhead catfish @en")));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), not(containsString("bagre boca chica @en")));
    }

    @Test
    public void lookupWoRMSCod() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Gadus morhua");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), containsString("Animalia | Chordata | Actinopterygii | Gadiformes | Gadidae | Gadus | Gadus morhua"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    private Map<String, String> assertHomoSapiens(GlobalNamesService service) throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Hominidae | Homo | Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:914181 | ITIS:179913 | ITIS:179916 | ITIS:179925 | ITIS:180089 | ITIS:180090 | ITIS:180091 | ITIS:180092"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Order | Family | Genus | Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
        return enrich;
    }

    @Test
    public void lookupITISNonExisting() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Donald Duck");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.size(), is(0));
    }

    @Test
    public void lookupITISFish() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        service.setIncludeCommonNames(true);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Ariopsis felis");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Osteichthyes | Actinopterygii | Neopterygii | Teleostei | Ostariophysi | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:161030 | ITIS:161061 | ITIS:553120 | ITIS:161105 | ITIS:162845 | ITIS:163992 | ITIS:164157 | ITIS:639019 | ITIS:680665"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Superorder | Order | Family | Genus | Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:680665"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is("bagre boca chica @Spanish"));
    }

    @Test
    public void lookupGBIF() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.GBIF);
        service.setIncludeCommonNames(true);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Anura");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Anura"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Amphibia | Anura"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:1 | GBIF:44 | GBIF:131 | GBIF:952"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("order"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:952"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Kikkers @nl"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Бесхвостые @ru"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), not(containsString("Frogs @en")));
    }

    @Test
    public void lookupWORMS() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Anura");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is(nullValue()));
    }
    
    @Test 
    public void lookupMultipleSources() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        final List<Taxon> taxa = new ArrayList<Taxon>();
        service.findTermsForNames(Arrays.asList("Homo sapiens"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon, boolean isExactMatch) {
                taxa.add(taxon);
            }
        }, Arrays.asList(GlobalNamesSources.GBIF, GlobalNamesSources.ITIS));

        assertThat(taxa.size(), is(2));

    }
}
