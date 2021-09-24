package org.eol.globi.data;

import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.util.ArrayList;
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
    public void associatedTaxaGaller() {
        List<Map<String, String>> properties = parseAssociatedTaxa("\"galler of\":\"Hypochaeris radicata L.\"");

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("galler of"));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Hypochaeris radicata L."));
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
    public void associatedTaxaMultiple2() {
        String associatedTaxa = "eats: Homo sapiens,Canis lupus";
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
    public void associatedTaxaFoundOn() {
        String associatedTaxa = "Found on wild turkey";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("wild turkey"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("Found on"));
    }

    @Test
    public void associatedTaxaFoundOn2() {
        String associatedTaxa = "found on overhang";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("overhang"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("found on"));
    }

    @Test
    public void associatedTaxaOn() {
        String associatedTaxa = "on inflorescence (past flowering) of Onosmodium molle";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("inflorescence (past flowering) of Onosmodium molle"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("on"));
    }

    @Test
    public void associatedTaxaOn2() {
        String associatedTaxa = "On Monarda punctata Oak-Pine Barrens";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Monarda punctata Oak-Pine Barrens"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("On"));
    }

    @Test
    public void associatedTaxaGrasping() {
        String associatedTaxa = "grasping dead Epicauta pensylvanica #4283 among flowers of Solidago speciosa 4PM fair 72";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("dead Epicauta pensylvanica #4283 among flowers of Solidago speciosa 4PM fair 72"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("grasping"));
    }


    @Test
    public void associatedTaxaCollectedFrom() {
        String associatedTaxa = "Coll. Sharp-shinned Hawk im. Female";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Sharp-shinned Hawk im. Female"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("Coll."));
    }

    @Test
    public void associatedTaxaCollectedFrom1() {
        String associatedTaxa = "Coll. in field with sweep net";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("field with sweep net"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("Coll. in"));
    }

    @Test
    public void associatedTaxaCollectedFrom2() {
        String associatedTaxa = "coll. from Ulmus americana";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Ulmus americana"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("coll. from"));
    }

    @Test
    public void associatedTaxaCollectedFrom3() {
        String associatedTaxa = "Collected from Yellow Pan Traps";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Yellow Pan Traps"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("Collected from"));
    }

    @Test
    public void associatedTaxaCollectedFrom4() {
        String associatedTaxa = "collected from underbrush oak pasture";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("underbrush oak pasture"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("collected from"));
    }

    @Test
    public void associatedTaxaCollectedOn() {
        String associatedTaxa = "collected on Lathyrus japonicus";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Lathyrus japonicus"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("collected on"));
    }

    @Test
    public void associatedTaxaClutching() {
        String associatedTaxa = "clutching dead Xestia smithii #4299 among flowers of Spiraea alba noon sunny";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("dead Xestia smithii #4299 among flowers of Spiraea alba noon sunny"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("clutching"));
    }

    @Test
    public void associatedTaxaRearedFrom() {
        String associatedTaxa = "reared from Gravesia aqualegia";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Gravesia aqualegia"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("reared from"));
    }


    @Test
    public void associatedTaxaFeedingOn() {
        String associatedTaxa = "feeding on Northern Blue Butterfly";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Northern Blue Butterfly"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("feeding on"));
    }

    @Test
    public void associatedTaxaFrom() {
        String associatedTaxa = "From red tail hawk";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("red tail hawk"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("From"));
    }

    @Test
    public void associatedTaxaFrom2() {
        String associatedTaxa = "from turkey brooder house on Clem Stedley farm";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("turkey brooder house on Clem Stedley farm"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("from"));
    }

    @Test
    public void associatedTaxaFeedingOn2() {
        String associatedTaxa = "Feeding on domestic bronze turkey";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("domestic bronze turkey"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("Feeding on"));
    }

    @Test
    public void associatedTaxaFeedingOn3() {
        String associatedTaxa = "fem. feeding on dead Eristalis tenax #4571 among flowers of Eupatorium perfoliatum w/ male atop her";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("dead Eristalis tenax #4571 among flowers of Eupatorium perfoliatum w/ male atop her"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("feeding on"));
    }

    @Test
    public void associatedTaxaRestingOn() {
        String associatedTaxa = "resting on Juniperus virginiana trunk";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Juniperus virginiana trunk"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("resting on"));
    }

    @Test
    public void habitatParsing() {
        String associatedTaxa = "Exposed siliceous rocks";
        List<Map<String, String>> properties = parseAssociatedTaxa(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Exposed siliceous rocks"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is(""));
    }

    @Test
    public void associatedTaxaCollectedVar() {
        String associatedTaxa = "Coll. Im. Red-Tailed Hawk";
        List<Map<String, String>> properties = attemptParsingAssociationString(associatedTaxa);

        assertThat(properties.size(), is(1));
        assertThat(properties.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Im. Red-Tailed Hawk"));
        assertThat(properties.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(properties.get(0).get(INTERACTION_TYPE_NAME), is("Coll."));
    }

    public static List<Map<String, String>> attemptParsingAssociationString(String associatedTaxa) {
        List<Map<String, String>> properties = new ArrayList<>();
        return AssociatedTaxaUtil.attemptParsingAssociationString(associatedTaxa, properties);
    }

}