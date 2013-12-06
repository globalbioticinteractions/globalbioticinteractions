package org.eol.globi.data.taxon;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class GulfBaseTaxonParserTest {

    @Test
    public void readAllLines() throws IOException {
        final List<TaxonTerm> terms = new ArrayList<TaxonTerm>();
        Map<String, BufferedReader> allReaders = new GulfBaseTaxonReaderFactory().getAllReaders();
        TaxonParser taxonParser = new GulfBaseTaxonParser();
        TestTaxonImportListener listener = new TestTaxonImportListener(terms);

        for (Map.Entry<String, BufferedReader> entry : allReaders.entrySet()) {
            try {
                taxonParser.parse(entry.getValue(), listener);
            } catch (IOException ex) {
                throw new IOException("problem parsing reader with name [" + entry.getKey() + "]");
            }
        }

        assertThat(terms.size(), is(10));

        TaxonTerm taxonTerm = terms.get(0);
        assertThat(taxonTerm.getId(), is("Spp-26-0003"));
        assertThat(taxonTerm.getRank(), is(nullValue()));
        assertThat(taxonTerm.getName(), is("Haplognathia rosea"));
        assertThat(taxonTerm.getRankPath(), is("Animalia Gnathostomulida Filospermoidea Haplognathiidae Haplognathia"));

        taxonTerm = terms.get(2);
        assertThat(taxonTerm.getId(), is("Spp-26-0005"));
        assertThat(taxonTerm.getRank(), is(nullValue()));
        assertThat(taxonTerm.getName(), is("Haplognathia cf. ruberrima"));
        assertThat(taxonTerm.getRankPath(), is("Animalia Gnathostomulida Filospermoidea Haplognathiidae Haplognathia"));

        assertThat(listener.count, is(taxonParser.getExpectedMaxTerms()));
    }

    @Test
    public void readThreeLine() throws IOException {

        BufferedReader threeFirstLinesFromAcanthocephala_O = new BufferedReader(new StringReader("Species number,Scientific name,Kingdom,Phylum,Subphylum,Class,Subclass,Infraclass,Superorder,Order,Suborder,Infraorder,Section,Subsection,Superfamily,Above family,Family,Subfamily,Tribe,Supergenus,Genus,Subgenus,Species,Subspecies,Synonyms,Scientific name author,Habitat-Biology,Overall geographic range,Min depth (m),Max depth (m),Polygon,Source,References,Endnotes,Author,Year,Changes from the book,URL\n" +
                "Spp-28-0004,Caballerorhynchus lamothei,Animalia,Acanthocephala,,Palaeacanthocephala,,,,Echinorhynchida,,,,,,,Cavisomidae,,,,Caballerorhynchus,,lamothei,,,\"Salgado-Maldonado,\",parasitic,Coastal waters and tidal wetlands southwest Gulf of Mexico,,,\"E1; E2; E3; E4; E5; E6\",\"Salgado-Maldonado, G. and O. M. Amin. 2009. Acanthocephala of the Gulf of Mexico, Pp. 539–552 in Felder, D.L. and D.K. Camp (eds.), Gulf of Mexico–Origins, Waters, and Biota. Biodiversity. Texas A&M Press, College Station, Texas.\",111,1,Salgado-Maldonado,1977,\"Added keyword \"\"parasitic\"\"\",http://gulfbase.org/biogomx/biospecies.php?species=Spp-28-0004\n" +
                "Spp-28-0005,Caballerorhynchus lamothei,Animalia,Acanthocephala,,Palaeacanthocephala,,,,Echinorhynchida,,,,,,,Cavisomidae,,,,Caballerorhynchus,,lamothei,,,\"Salgado-Maldonado,\",parasitic,Coastal waters and tidal wetlands southwest Gulf of Mexico,,,\"E1; E2; E3; E4; E5; E6\",\"Salgado-Maldonado, G. and O. M. Amin. 2009. Acanthocephala of the Gulf of Mexico, Pp. 539–552 in Felder, D.L. and D.K. Camp (eds.), Gulf of Mexico–Origins, Waters, and Biota. Biodiversity. Texas A&M Press, College Station, Texas.\",103,,Salgado-Maldonado,1977,\"Added keyword \"\"parasitic\"\"\",http://gulfbase.org/biogomx/biospecies.php?species=Spp-28-0005\n" +
                "Spp-28-0007,Filisoma fidum,Animalia,Acanthocephala,,Palaeacanthocephala,,,,Echinorhynchida,,,,,,,Cavisomidae,,,,Filisoma,,fidum,,,\"Van Cleave and Manter,\",\"parasitic; endemic to Gulf of Mexico\",Known only from Dry Tortugas Florida,,,\"C1; C2; C3; C4; C5; C6\",\"Salgado-Maldonado, G. and O. M. Amin. 2009. Acanthocephala of the Gulf of Mexico, Pp. 539–552 in Felder, D.L. and D.K. Camp (eds.), Gulf of Mexico–Origins, Waters, and Biota. Biodiversity. Texas A&M Press, College Station, Texas.\",152,,Van Cleave & Manter,1947,\"Added keyword \"\"endemic to Gulf of Mexico\"\" and \"\"parasitic\"\"\",http://gulfbase.org/biogomx/biospecies.php?species=Spp-28-0007"));

        assertThat(threeFirstLinesFromAcanthocephala_O, is(notNullValue()));


        TaxonParser taxonParser = new GulfBaseTaxonParser();
        final List<TaxonTerm> terms = new ArrayList<TaxonTerm>();
        TestTaxonImportListener listener = new TestTaxonImportListener(terms);
        taxonParser.parse(threeFirstLinesFromAcanthocephala_O, listener);

        assertThat(terms.size(), is(3));

        TaxonTerm taxonTerm = terms.get(0);
        assertThat(taxonTerm.getId(), is("Spp-28-0004"));
        assertThat(taxonTerm.getRank(), is(nullValue()));
        assertThat(taxonTerm.getName(), is("Caballerorhynchus lamothei"));
        assertThat(taxonTerm.getRankPath(), is("Animalia Acanthocephala Palaeacanthocephala Echinorhynchida Cavisomidae Caballerorhynchus"));

        taxonTerm = terms.get(2);
        assertThat(taxonTerm.getId(), is("Spp-28-0007"));
        assertThat(taxonTerm.getRank(), is(nullValue()));
        assertThat(taxonTerm.getName(), is("Filisoma fidum"));
        assertThat(taxonTerm.getRankPath(), is("Animalia Acanthocephala Palaeacanthocephala Echinorhynchida Cavisomidae Filisoma"));
    }

    private static class TestTaxonImportListener implements TaxonImportListener {
        private final List<TaxonTerm> terms;
        int count = 0;

        public TestTaxonImportListener(List<TaxonTerm> terms) {
            this.terms = terms;
        }

        @Override
        public void addTerm(TaxonTerm taxonTerm) {
            if (terms.size() < 10) {
                terms.add(taxonTerm);
            }
            count++;
        }

        @Override
        public void addTerm(String name, TaxonTerm term) {

        }

        @Override
        public void start() {

        }

        @Override
        public void finish() {

        }
    }

}
