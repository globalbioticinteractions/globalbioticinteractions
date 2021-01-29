package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.eol.globi.data.AssociatedTaxaUtil.parseAssociatedTaxa;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AssociatedTaxaUtilTest {

    @Test
    public void associatedTaxaMultipleBlanks2() {
        String associatedTaxa = "V. priceana, , V. papilionacea";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(2));
    }

    @Test
    public void associatedTaxaMultipleCommas() {
        String associatedTaxa = "Ceramium, Chaetomorpha linum, Enteromorpha intestinalis, Ulva angusta, Porphyra perforata, Sargassum muticum, Gigartina spp., Rhodoglossum affine, and Grateloupia sp.";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(9));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Ceramium"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is(""));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(8).get(TaxonUtil.TARGET_TAXON_NAME), is("and Grateloupia sp."));
        assertThat(properties.get(8).get(INTERACTION_TYPE_NAME), is(""));
        assertThat(properties.get(8).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxa2() {
        String associatedTaxa = " visitsFlowersOf:Eschscholzia californica";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Eschscholzia californica"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("visitsFlowersOf"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxa3() {
        String associatedTaxa = " visitsFlowersOf : Lupinus succulentus";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Lupinus succulentus"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("visitsFlowersOf"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxa4() {
        String associatedTaxa = "visitsFlowersOf: Phallia";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Phallia"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("visitsFlowersOf"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaMultiple() {
        String associatedTaxa = "eats: Homo sapiens | eats: Canis lupus";
        assertTwoInteractions(associatedTaxa);
    }

    @Test
    public void associatedTaxaEx() {
        String associatedTaxa = "ex Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("ex Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("ex"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaRearedEx() {
        String associatedTaxa = "ReAred ex Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("ReAred ex Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("reared ex"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxonomicHierachy() {
        String associatedTaxa = "Caesalpinaceae: Cercidium: praecox";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Cercidium praecox"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is(""));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxonomicHierachy2() {
        String associatedTaxa = "Bucephala albeola: Anatidae";

        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Bucephala albeola"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is(""));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaExPeriod() {
        String associatedTaxa = "ex. Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("ex. Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("ex"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaMultipleBlanks() {
        String associatedTaxa = "Homo sapiens, ,Felis catus";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(2));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is(""));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(1).get(TaxonUtil.TARGET_TAXON_NAME), is("Felis catus"));
        assertThat(properties.get(1).get(INTERACTION_TYPE_NAME), is(""));
        assertThat(properties.get(1).get(INTERACTION_TYPE_ID), is(nullValue()));
    }


    @Test
    public void associatedTaxaMixed() {
        String associatedTaxa = "Ceramium, Chaetomorpha linum| Enteromorpha intestinalis; Ulva angusta, Porphyra perforata, Sargassum muticum, Gigartina spp., Rhodoglossum affine, and Grateloupia sp.";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(9));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Ceramium"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is(""));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(8).get(TaxonUtil.TARGET_TAXON_NAME), is("and Grateloupia sp."));
        assertThat(properties.get(8).get(INTERACTION_TYPE_NAME), is(""));
        assertThat(properties.get(8).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaMultipleSemicolon() {
        String associatedTaxa = "eats: Homo sapiens ; eats: Canis lupus";
        assertTwoInteractions(associatedTaxa);
    }

    private void assertTwoInteractions(String associatedTaxa) {
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(2));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(1).get(TaxonUtil.TARGET_TAXON_NAME), is("Canis lupus"));
        assertThat(properties.get(1).get(INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(1).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaUnsupported() {
        String associatedTaxa = "eatz: Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("eatz"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxa() {
        String associatedTaxa = "eats: Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaQuoted() {
        String associatedTaxa = "\"eats\": \"Homo sapiens\"";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaEscapedQuoted() {
        String associatedTaxa = "\\\"eats\\\": \\\"Homo sapiens\\\"";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("eats"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
    }

    @Test
    public void associatedTaxaBlank() {
        String associatedTaxa = "Homo sapiens";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Homo sapiens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is(""));
    }

    @Test
    public void associatedTaxaVisiting() {
        String associatedTaxa = "Caught after visiting Calopogon pallidus";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Caught after visiting Calopogon pallidus"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(InteractType.VISITS));
    }

    @Test
    public void associatedTaxaCaughtOn() {
        String associatedTaxa = "Caught on Aletris farinosa";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Caught on Aletris farinosa"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(InteractType.INTERACTS_WITH));
    }

    @Test
    public void associatedTaxaVisiting2() {
        String associatedTaxa = "Visiting Pogonia ophioglossoides";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Visiting Pogonia ophioglossoides"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(InteractType.VISITS));
    }

    @Test
    public void associatedTaxaVisiting3() {
        String associatedTaxa = "Visitiing Baptisia tinctoria";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Visitiing Baptisia tinctoria"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(InteractType.VISITS));
    }

    @Test
    public void associatedTaxaVisiting4() {
        String associatedTaxa = "Visited by Hyles lineata hawkmoths.";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Visited by Hyles lineata hawkmoths."));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(InteractType.VISITED_BY));
    }

    @Test
    public void associatedTaxaVisiting5() {
        String associatedTaxa = "visited by Hyles lineata hawkmoths.";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("visited by Hyles lineata hawkmoths."));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(InteractType.VISITED_BY));
    }


}