package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResultFormatterDOTTest {

    @Test
    public void idTest() {
        assertThat(ResultFormatterDOT.getSafeLabel("EOL:123"), Is.is("EOL_123"));
        assertThat(ResultFormatterDOT.getSafeLabel("EOL//123"), Is.is("EOL__123"));
        assertThat(ResultFormatterDOT.getSafeLabel("Animalia | Primates"), Is.is("Animalia___Primates"));
    }

    @Test
    public void formatter() throws ResultFormattingException {
        String dots = new ResultFormatterDOT().format("{\n" +
                "  \"columns\" : [ \"source_taxon_name\", \"interaction_type\", \"target_taxon_name\" ],\n" +
                "  \"data\" : [ [ \"Vireo olivaceus\", \"preysOn\", [ \"Diptera\", \"Auchenorrhyncha\", \"Coleoptera\", \"Lepidoptera\", \"Arachnida\" ] ], [ \"Colaptes auratus\", \"preysOn\", [ \"Hymenoptera\" ] ], [ \"Anseriformes\", \"preysOn\", [ \"Cyperaceae\", \"Tracheophyta\", \"marine invertebrates\" ] ], [ \"Geositta\", \"preysOn\", [ \"Insecta\", \"Coelopidae\", \"Diptera\", \"Talitridae\" ] ], [ \"Patagioenas squamosa\", \"preysOn\", [ \"fruit\" ] ], [ \"Ardea cinerea\", \"preysOn\", [ \"Anguilla anguilla\", \"Pollachius virens\", \"Ammodytes tobianus\", \"Zoarces viviparus\", \"Pholis gunnellus\", \"Myoxocephalus scorpius\", \"Pomatoschistus microps\", \"Crangon crangon\", \"Salmo trutta\" ] ], [ \"Setophaga petechia\", \"preysOn\", [ \"Araneae\", \"Insecta\", \"Orthoptera\", \"fruit and seeds\", \"Hemiptera\", \"Diptera\", \"Lepidoptera\", \"Formicidae\", \"Hymenoptera\", \"Coleoptera\", \"Auchenorrhyncha\" ] ] ] }");
        assertThat(dots, is(notNullValue()));
        assertThat(dots, containsString("Vireo_olivaceus->Diptera[label=\"preysOn\"];"));
    }

    @Test
    public void formatterWithResultsWrapper() throws ResultFormattingException {
        String dots = new ResultFormatterDOT().format("{\"results\":[{\"columns\":[\"source_taxon_external_id\",\"source_taxon_name\",\"source_taxon_path\",\"source_specimen_occurrence_id\",\"source_specimen_institution_code\",\"source_specimen_collection_code\",\"source_specimen_catalog_number\",\"source_specimen_life_stage\",\"source_specimen_basis_of_record\",\"interaction_type\",\"target_taxon_external_id\",\"target_taxon_name\",\"target_taxon_path\",\"target_specimen_occurrence_id\",\"target_specimen_institution_code\",\"target_specimen_collection_code\",\"target_specimen_catalog_number\",\"target_specimen_life_stage\",\"target_specimen_basis_of_record\",\"latitude\",\"longitude\",\"study_title\"],\"data\":[{\"row\":[\"EOL:2586535\",\"Acari\",\"Life | Cellular Organisms | Eukaryota | Opisthokonta | Metazoa | Bilateria | Protostomia | Ecdysozoa | Arthropoda | Chelicerata | Arachnida | Acari\",null,null,null,null,null,null,\"hasHost\",\"EOL:346596\",\"Heteromys desmarestianus\",\"Life | Cellular Organisms | Eukaryota | Opisthokonta | Metazoa | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Osteichthyes | Sarcopterygii | Tetrapoda | Amniota | Synapsida | Therapsida | Cynodontia | Mammalia | Theria | Eutheria | Placentalia | Boreoeutheria | Euarchontoglires | Glires | Rodentia | Mouse relatives | Castorimorpha | Geomyoidea | Heteromyidae | Heteromyinae | Heteromys | Heteromys desmarestianus\",null,null,null,null,null,null,null,null,null],\"meta\":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null]},{\"row\":[\"EOL:346596\",\"Heteromys desmarestianus\",\"Life | Cellular Organisms | Eukaryota | Opisthokonta | Metazoa | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Osteichthyes | Sarcopterygii | Tetrapoda | Amniota | Synapsida | Therapsida | Cynodontia | Mammalia | Theria | Eutheria | Placentalia | Boreoeutheria | Euarchontoglires | Glires | Rodentia | Mouse relatives | Castorimorpha | Geomyoidea | Heteromyidae | Heteromyinae | Heteromys | Heteromys desmarestianus\",null,null,null,null,null,null,\"hostOf\",\"EOL:2586535\",\"Acari\",\"Life | Cellular Organisms | Eukaryota | Opisthokonta | Metazoa | Bilateria | Protostomia | Ecdysozoa | Arthropoda | Chelicerata | Arachnida | Acari\",null,null,null,null,null,null,null,null,null],\"meta\":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null]}]}],\"errors\":[]}");
        assertThat(dots, is(notNullValue()));
        assertThat(dots, containsString("Acari->Heteromys_desmarestianus[label=\"hasHost\"];"));
    }

    @Test
    public void formatterNoColumns() throws ResultFormattingException {
        String dots = new ResultFormatterDOT().format("{\n" +
                "  \"columnz\" : [ \"source_taxon_name\", \"interaction_type\", \"target_taxon_name\" ],\n" +
                "  \"data\" : [ [ \"Vireo olivaceus\", \"preysOn\", [ \"Diptera\", \"Auchenorrhyncha\", \"Coleoptera\", \"Lepidoptera\", \"Arachnida\" ] ], [ \"Colaptes auratus\", \"preysOn\", [ \"Hymenoptera\" ] ], [ \"Anseriformes\", \"preysOn\", [ \"Cyperaceae\", \"Tracheophyta\", \"marine invertebrates\" ] ], [ \"Geositta\", \"preysOn\", [ \"Insecta\", \"Coelopidae\", \"Diptera\", \"Talitridae\" ] ], [ \"Patagioenas squamosa\", \"preysOn\", [ \"fruit\" ] ], [ \"Ardea cinerea\", \"preysOn\", [ \"Anguilla anguilla\", \"Pollachius virens\", \"Ammodytes tobianus\", \"Zoarces viviparus\", \"Pholis gunnellus\", \"Myoxocephalus scorpius\", \"Pomatoschistus microps\", \"Crangon crangon\", \"Salmo trutta\" ] ], [ \"Setophaga petechia\", \"preysOn\", [ \"Araneae\", \"Insecta\", \"Orthoptera\", \"fruit and seeds\", \"Hemiptera\", \"Diptera\", \"Lepidoptera\", \"Formicidae\", \"Hymenoptera\", \"Coleoptera\", \"Auchenorrhyncha\" ] ] ] }");
        assertThat(dots, is(notNullValue()));
    }

    @Test
    public void formatterNoRows() throws ResultFormattingException {
        String dots = new ResultFormatterDOT().format("{\n" +
                "  \"columns\" : [ \"source_taxon_name\", \"interaction_type\", \"target_taxon_name\" ],\n" +
                "  \"dataz\" : [ [ \"Vireo olivaceus\", \"preysOn\", [ \"Diptera\", \"Auchenorrhyncha\", \"Coleoptera\", \"Lepidoptera\", \"Arachnida\" ] ], [ \"Colaptes auratus\", \"preysOn\", [ \"Hymenoptera\" ] ], [ \"Anseriformes\", \"preysOn\", [ \"Cyperaceae\", \"Tracheophyta\", \"marine invertebrates\" ] ], [ \"Geositta\", \"preysOn\", [ \"Insecta\", \"Coelopidae\", \"Diptera\", \"Talitridae\" ] ], [ \"Patagioenas squamosa\", \"preysOn\", [ \"fruit\" ] ], [ \"Ardea cinerea\", \"preysOn\", [ \"Anguilla anguilla\", \"Pollachius virens\", \"Ammodytes tobianus\", \"Zoarces viviparus\", \"Pholis gunnellus\", \"Myoxocephalus scorpius\", \"Pomatoschistus microps\", \"Crangon crangon\", \"Salmo trutta\" ] ], [ \"Setophaga petechia\", \"preysOn\", [ \"Araneae\", \"Insecta\", \"Orthoptera\", \"fruit and seeds\", \"Hemiptera\", \"Diptera\", \"Lepidoptera\", \"Formicidae\", \"Hymenoptera\", \"Coleoptera\", \"Auchenorrhyncha\" ] ] ] }");
        assertThat(dots, is(notNullValue()));
    }

    @Test
    public void formatterNotSourceTaxon() throws ResultFormattingException {
        String dots = new ResultFormatterDOT().format("{\n" +
                "  \"columns\" : [ \"source_taxon_namez\", \"interaction_type\", \"target_taxon_namez\" ],\n" +
                "  \"data\" : [ [ \"Vireo olivaceus\", \"preysOn\", [ \"Diptera\", \"Auchenorrhyncha\", \"Coleoptera\", \"Lepidoptera\", \"Arachnida\" ] ], [ \"Colaptes auratus\", \"preysOn\", [ \"Hymenoptera\" ] ], [ \"Anseriformes\", \"preysOn\", [ \"Cyperaceae\", \"Tracheophyta\", \"marine invertebrates\" ] ], [ \"Geositta\", \"preysOn\", [ \"Insecta\", \"Coelopidae\", \"Diptera\", \"Talitridae\" ] ], [ \"Patagioenas squamosa\", \"preysOn\", [ \"fruit\" ] ], [ \"Ardea cinerea\", \"preysOn\", [ \"Anguilla anguilla\", \"Pollachius virens\", \"Ammodytes tobianus\", \"Zoarces viviparus\", \"Pholis gunnellus\", \"Myoxocephalus scorpius\", \"Pomatoschistus microps\", \"Crangon crangon\", \"Salmo trutta\" ] ], [ \"Setophaga petechia\", \"preysOn\", [ \"Araneae\", \"Insecta\", \"Orthoptera\", \"fruit and seeds\", \"Hemiptera\", \"Diptera\", \"Lepidoptera\", \"Formicidae\", \"Hymenoptera\", \"Coleoptera\", \"Auchenorrhyncha\" ] ] ] }");
        assertThat(dots, is(notNullValue()));
    }

    @Test
    public void formatterTaxonPathOnly() throws ResultFormattingException {
        String dots = new ResultFormatterDOT().format("{\n" +
                "  \"columns\" : [ \"source_taxon_path\", \"interaction_type\", \"target_taxon_path\" ],\n" +
                "  \"data\" : [ [ \"bla | Vireo olivaceus\", \"preysOn\", [ \"Diptera\", \"Auchenorrhyncha\", \"Coleoptera\", \"Lepidoptera\", \"Arachnida\" ] ], [ \"Colaptes auratus\", \"preysOn\", [ \"Hymenoptera\" ] ], [ \"Anseriformes\", \"preysOn\", [ \"Cyperaceae\", \"Tracheophyta\", \"marine invertebrates\" ] ], [ \"Geositta\", \"preysOn\", [ \"Insecta\", \"Coelopidae\", \"Diptera\", \"Talitridae\" ] ], [ \"Patagioenas squamosa\", \"preysOn\", [ \"fruit\" ] ], [ \"Ardea cinerea\", \"preysOn\", [ \"Anguilla anguilla\", \"Pollachius virens\", \"Ammodytes tobianus\", \"Zoarces viviparus\", \"Pholis gunnellus\", \"Myoxocephalus scorpius\", \"Pomatoschistus microps\", \"Crangon crangon\", \"Salmo trutta\" ] ], [ \"Setophaga petechia\", \"preysOn\", [ \"Araneae\", \"Insecta\", \"Orthoptera\", \"fruit and seeds\", \"Hemiptera\", \"Diptera\", \"Lepidoptera\", \"Formicidae\", \"Hymenoptera\", \"Coleoptera\", \"Auchenorrhyncha\" ] ] ] }");
        assertThat(dots, is(notNullValue()));
    }

    @Test
    public void formatterInteractionsQuery() throws IOException {
        String dots = new ResultFormatterDOT()
                .format(IOUtils.toString(getClass().getResourceAsStream("interactions-result.json"), StandardCharsets.UTF_8));
        assertThat(dots, is(notNullValue()));
    }


}
