package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GlobalNamesServiceTest {

    @Test
    public void createTaxaListFromNameList() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        service.findTermsForNames(Arrays.asList("1|Homo sapiens", "2|Ariopsis felis"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon) {
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
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
    }

    @Test
    public void lookupNCBI() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.NCBI);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("|Eukaryota|Opisthokonta|Metazoa|Eumetazoa|Bilateria|Coelomata|Deuterostomia|Chordata|Craniata|Vertebrata|Gnathostomata|Teleostomi|Euteleostomi|Sarcopterygii|Tetrapoda|Amniota|Mammalia|Theria|Eutheria|Euarchontoglires|Primates|Haplorrhini|Simiiformes|Catarrhini|Hominoidea|Hominidae|Homininae|Homo|Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("|superkingdom||kingdom|||||phylum|subphylum||superclass||||||class|||superorder|order|suborder|infraorder|parvorder|superfamily|family|subfamily|genus|species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
    }

    @Test
    public void lookupWoRMS() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Ariopsis felis");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia|Chordata|Actinopterygii|Siluriformes|Ariidae|Ariopsis|Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("WORMS:158709"));
    }

    private Map<String, String> assertHomoSapiens(GlobalNamesService service) throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia|Bilateria|Deuterostomia|Chordata|Vertebrata|Gnathostomata|Tetrapoda|Mammalia|Theria|Eutheria|Primates|Hominidae|Homo|Homo sapiens"));
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
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Ariopsis felis");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia|Bilateria|Deuterostomia|Chordata|Vertebrata|Gnathostomata|Osteichthyes|Actinopterygii|Neopterygii|Teleostei|Ostariophysi|Siluriformes|Ariidae|Ariopsis|Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:680665"));
    }

    @Test
    public void lookupGBIF() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.GBIF);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Anura");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Anura"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia|Chordata|Amphibia|Anura"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("order"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:952"));
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
            public void foundTaxonForName(Long id, String name, Taxon taxon) {
                taxa.add(taxon);
            }
        }, Arrays.asList(GlobalNamesSources.GBIF, GlobalNamesSources.ITIS));

        assertThat(taxa.size(), is(2));

    }
}
