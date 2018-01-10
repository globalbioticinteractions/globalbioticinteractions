package org.eol.globi.export;

import org.eol.globi.data.NodeFactoryException;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExporterTaxaTest {

    @Test
    public void includeHigherOrderRanks() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("rank", "the taxon rank");
                put("pathNames", "kingdom | phylum | class | order | family | genus");
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("scientificName", "Some namus");
                put("taxonId", "EOL:1234");
            }
        };
        assertHigherOrdersPresent(result);
    }

    @Test
    public void includeHigherOrderRanksUpperCase() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("rank", "the taxon rank");
                put("pathNames", "Kingdom | Phylum | Class | Order | Family | Genus");
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("scientificName", "Some namus");
                put("taxonId", "EOL:1234");
            }
        };
        assertHigherOrdersPresent(result);
    }

    @Test
    public void includeHigherOrderRanksMissingTaxa() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("rank", "Species");
                put("pathNames", "| superkingdom | kingdom | phylum |  |  |  |  |  |  |  |  |  | order | family | genus | species");
                put("path", "Cellular organisms | Eukaryota | Viridiplantae | Streptophyta | Streptophytina | Embryophyta | Tracheophyta | Euphyllophyta | Spermatophyta | Magnoliophyta | Mesangiospermae | Eudicotyledons | Stem eudicotyledons | Proteales | Proteaceae | Persoonia | Persoonia linearis");
                put("scientificName", "Persoonia linearis");
                put("taxonId", "EOL:2927271");
            }
        };
        HashMap<String, String> rowFields = new HashMap<String, String>();
        ExporterTaxa.resultsToRow(rowFields, result);

        assertThat(rowFields.get(EOLDictionary.TAXON_ID), is("EOL:2927271"));
        assertThat(rowFields.get(EOLDictionary.SCIENTIFIC_NAME), is("Persoonia linearis"));
        assertThat(rowFields.get(EOLDictionary.TAXON_RANK), is("species"));
        assertThat(rowFields.get(EOLDictionary.KINGDOM), is("Viridiplantae"));
        assertThat(rowFields.get(EOLDictionary.PHYLUM), is("Streptophyta"));
        assertThat(rowFields.get(EOLDictionary.CLASS), is(nullValue()));
        assertThat(rowFields.get(EOLDictionary.ORDER), is("Proteales"));
        assertThat(rowFields.get(EOLDictionary.FAMILY), is("Proteaceae"));
        assertThat(rowFields.get(EOLDictionary.GENUS), is("Persoonia"));
        assertThat(rowFields.get(EOLDictionary.FURTHER_INFORMATION_URL), is("http://eol.org/pages/2927271"));
    }

    public void assertHigherOrdersPresent(HashMap<String, Object> result) {
        HashMap<String, String> rowFields = new HashMap<String, String>();
        ExporterTaxa.resultsToRow(rowFields, result);

        assertThat(rowFields.get(EOLDictionary.TAXON_ID), is("EOL:1234"));
        assertThat(rowFields.get(EOLDictionary.SCIENTIFIC_NAME), is("Some namus"));
        assertThat(rowFields.get(EOLDictionary.TAXON_RANK), is("the taxon rank"));
        assertThat(rowFields.get(EOLDictionary.KINGDOM), is("the kingdom"));
        assertThat(rowFields.get(EOLDictionary.PHYLUM), is("the phylum"));
        assertThat(rowFields.get(EOLDictionary.CLASS), is("the class"));
        assertThat(rowFields.get(EOLDictionary.ORDER), is("the order"));
        assertThat(rowFields.get(EOLDictionary.FAMILY), is("the family"));
        assertThat(rowFields.get(EOLDictionary.GENUS), is("the genus"));
        assertThat(rowFields.get(EOLDictionary.FURTHER_INFORMATION_URL), is("http://eol.org/pages/1234"));
    }

    @Test
    public void missingTaxonPathnames() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("taxonRankz", "the taxon rank");
                put("pathNames", null);
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("scientificName", "Some namus");
                put("taxonId", "ZZZ:1234");
            }
        };
        HashMap<String, String> rowFields = new HashMap<String, String>();
        ExporterTaxa.resultsToRow(rowFields, result);

        assertThat(rowFields.get(EOLDictionary.TAXON_ID), is("ZZZ:1234"));
        assertThat(rowFields.get(EOLDictionary.SCIENTIFIC_NAME), is("Some namus"));
        assertThat(rowFields.containsKey(EOLDictionary.TAXON_RANK), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.KINGDOM), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.PHYLUM), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.CLASS), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.ORDER), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.FAMILY), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.GENUS), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.FURTHER_INFORMATION_URL), is(false));
    }

    @Test
    public void includeInvalidHigherOrderRanks() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("taxonRankz", "the taxon rank");
                put("pathNames", "kingdomz | phylum | classez | orderz | family | genuszz");
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("scientificName", "Some namus");
                put("taxonId", "ZZZ:1234");
            }
        };
        HashMap<String, String> rowFields = new HashMap<String, String>();
        ExporterTaxa.resultsToRow(rowFields, result);

        assertThat(rowFields.get(EOLDictionary.TAXON_ID), is("ZZZ:1234"));
        assertThat(rowFields.get(EOLDictionary.SCIENTIFIC_NAME), is("Some namus"));
        assertThat(rowFields.containsKey(EOLDictionary.TAXON_RANK), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.KINGDOM), is(false));
        assertThat(rowFields.get(EOLDictionary.PHYLUM), is("the phylum"));
        assertThat(rowFields.containsKey(EOLDictionary.CLASS), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.ORDER), is(false));
        assertThat(rowFields.get(EOLDictionary.FAMILY), is("the family"));
        assertThat(rowFields.containsKey(EOLDictionary.GENUS), is(false));
        assertThat(rowFields.containsKey(EOLDictionary.FURTHER_INFORMATION_URL), is(false));
    }

    @Test
    public void infraSpecies() throws NodeFactoryException, IOException {
        HashMap<String, Object> result = new HashMap<String, Object>() {
            {
                put("rank", "Infraspecies");
                put("pathNames", "kingdom | phylum | class | order | family | genus | species | infraspecies");
                put("path", "Animalia | Chordata | Mammalia | Carnivora | Mustelidae | Enhydra | Enhydra lutris | Enhydra lutris nereis");
                put("scientificName", "Enhydra lutris nereis");
                put("taxonId", "EOL:1251487");
            }
        };
        HashMap<String, String> rowFields = new HashMap<String, String>();
        ExporterTaxa.resultsToRow(rowFields, result);

        assertThat(rowFields.get(EOLDictionary.TAXON_ID), is("EOL:1251487"));
        assertThat(rowFields.get(EOLDictionary.SCIENTIFIC_NAME), is("Enhydra lutris nereis"));
        // see https://github.com/jhpoelen/eol-globi-data/issues/114
        assertThat(rowFields.get(EOLDictionary.TAXON_RANK), is("Subspecies"));
        assertThat(rowFields.get(EOLDictionary.KINGDOM), is("Animalia"));
        assertThat(rowFields.get(EOLDictionary.PHYLUM), is("Chordata"));
        assertThat(rowFields.get(EOLDictionary.CLASS), is("Mammalia"));
        assertThat(rowFields.get(EOLDictionary.ORDER), is("Carnivora"));
        assertThat(rowFields.get(EOLDictionary.FAMILY), is("Mustelidae"));
        assertThat(rowFields.get(EOLDictionary.GENUS), is("Enhydra"));
    }

}