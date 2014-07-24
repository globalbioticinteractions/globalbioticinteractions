package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GlobalNamesServiceIT {

    @Test
    public void createTaxaListFromNameList() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService();
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        service.findTermsForNames(Arrays.asList("1|Homo sapiens", "2|Ariopsis felis"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon) {
                assertNotNull(id);
                foundTaxa.add(taxon);
            }
        });

        assertThat(foundTaxa.size(), is(2));
    }


    @Test
    public void lookupITIS() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = assertHomoSapiens(service);
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsid:itis.gov:itis_tsn:180092"));
    }

    @Test
    public void lookupITISSynonym() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        service.lookupPropertiesByName("Corizidae", props);
        assertThat(props.get(PropertyAndValueDictionary.NAME), is("Rhopalidae"));
        assertThat(props.get(PropertyAndValueDictionary.PATH), is("Animalia|Bilateria|Protostomia|Ecdysozoa|Arthropoda|Hexapoda|Insecta|Pterygota|Neoptera|Paraneoptera|Hemiptera|Heteroptera|Pentatomomorpha|Coreoidea|Rhopalidae"));
        assertThat(props.get(PropertyAndValueDictionary.RANK), is("Family"));
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsid:itis.gov:itis_tsn:108477"));
    }

    @Test
    public void lookupNCBI() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.NCBI);
        HashMap<String, String> props1 = new HashMap<String, String>();
        service.lookupPropertiesByName("Homo sapiens", props1);
        assertThat(props1.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(props1.get(PropertyAndValueDictionary.PATH), is("|Eukaryota|Opisthokonta|Metazoa|Eumetazoa|Bilateria|Coelomata|Deuterostomia|Chordata|Craniata|Vertebrata|Gnathostomata|Teleostomi|Euteleostomi|Sarcopterygii|Tetrapoda|Amniota|Mammalia|Theria|Eutheria|Euarchontoglires|Primates|Haplorrhini|Simiiformes|Catarrhini|Hominoidea|Hominidae|Homininae|Homo|Homo sapiens"));
        assertThat(props1.get(PropertyAndValueDictionary.PATH_NAMES), is("|superkingdom||kingdom|||||phylum|subphylum||superclass||||||class|||superorder|order|suborder|infraorder|parvorder|superfamily|family|subfamily|genus|species"));
        assertThat(props1.get(PropertyAndValueDictionary.RANK), is("species"));
        HashMap<String, String> props = props1;
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ncbi:9606"));
    }

    @Test
    public void lookupWoRMS() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props1 = new HashMap<String, String>();
        service.lookupPropertiesByName("Ariopsis felis", props1);
        assertThat(props1.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(props1.get(PropertyAndValueDictionary.PATH), is("Animalia|Chordata|Actinopterygii|Siluriformes|Ariidae|Ariopsis|Ariopsis felis"));
        assertThat(props1.get(PropertyAndValueDictionary.RANK), is("species"));
        HashMap<String, String> props = props1;
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsid:marinespecies.org:taxname:158709"));
    }

    private HashMap<String, String> assertHomoSapiens(GlobalNamesService service) throws TaxonPropertyLookupServiceException {
        HashMap<String, String> props = new HashMap<String, String>();
        service.lookupPropertiesByName("Homo sapiens", props);
        assertThat(props.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(props.get(PropertyAndValueDictionary.PATH), is("Animalia|Bilateria|Deuterostomia|Chordata|Vertebrata|Gnathostomata|Tetrapoda|Mammalia|Theria|Eutheria|Primates|Hominidae|Homo|Homo sapiens"));
        assertThat(props.get(PropertyAndValueDictionary.RANK), is("Species"));
        return props;
    }

    @Test
    public void lookupITISNonExisting() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        service.lookupPropertiesByName("Donald Duck", props);
        assertThat(props.size(), is(0));
    }

    @Test
    public void lookupITISFish() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        service.lookupPropertiesByName("Ariopsis felis", props);
        assertThat(props.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(props.get(PropertyAndValueDictionary.PATH), is("Animalia|Bilateria|Deuterostomia|Chordata|Vertebrata|Gnathostomata|Osteichthyes|Actinopterygii|Neopterygii|Teleostei|Ostariophysi|Siluriformes|Ariidae|Ariopsis|Ariopsis felis"));
        assertThat(props.get(PropertyAndValueDictionary.RANK), is("Species"));
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("urn:lsid:itis.gov:itis_tsn:680665"));
    }

    @Test
    public void lookupGBIF() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.GBIF);
        HashMap<String, String> props = new HashMap<String, String>();
        service.lookupPropertiesByName("Anura", props);
        assertThat(props.get(PropertyAndValueDictionary.NAME), is("Anura"));
        assertThat(props.get(PropertyAndValueDictionary.PATH), is("Animalia|Chordata|Amphibia|Anura"));
        assertThat(props.get(PropertyAndValueDictionary.RANK), is("order"));
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:952"));
    }

    @Test
    public void lookupWORMS() throws TaxonPropertyLookupServiceException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props = new HashMap<String, String>();
        service.lookupPropertiesByName("Anura", props);
        assertThat(props.get(PropertyAndValueDictionary.NAME), is(nullValue()));
    }
}
