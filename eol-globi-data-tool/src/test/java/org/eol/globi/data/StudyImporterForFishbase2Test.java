package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForFishbase2Test extends GraphDBTestCase {

    @Test
    public void parsePredats() throws IOException {
        JsonNode predats = new ObjectMapper().readTree(PREDATS_JSON);
        Map<String, String> predatsMap = StudyImporterForFishbase2.parsePredatorPrey(predats, "FB:");
        assertThat(predatsMap.toString(), is("{interactionTypeId=http://purl.obolibrary.org/obo/RO_0002439, interactionTypeName=preysOn, localityName=Not stated., sourceLifeStage=juv./adults, sourceTaxonId=FB:457, studyTitle=FB:REF:84, targetLifeStage=recruits/juv., targetTaxonId=FB:2}"));
    }

    @Test
    public void parsePredatsSeaLifeBase() throws IOException {
        JsonNode predats = new ObjectMapper().readTree("{\n" +
                "      \"autoctr\": 24235,\n" +
                "      \"StockCode\": 26,\n" +
                "      \"SpecCode\": 83456,\n" +
                "      \"PredatsRefNo\": 97658,\n" +
                "      \"Locality\": \"eastern Australia\",\n" +
                "      \"C_Code\": \"036\",\n" +
                "      \"Predatstage\": \"recruits/juv.\",\n" +
                "      \"PredatorI\": \"finfish\",\n" +
                "      \"PredatorII\": \"bony fish\",\n" +
                "      \"PreyStage\": \"juv./adults\",\n" +
                "      \"PredatorGroup\": \"Scombridae\",\n" +
                "      \"DietP\": null,\n" +
                "      \"StomachContent\": null,\n" +
                "      \"PredatorName\": \"Euthynnus affinis\",\n" +
                "      \"Predatcode\": 96,\n" +
                "      \"PredatCodeDB\": \"FB\",\n" +
                "      \"AlphaCode\": null,\n" +
                "      \"MaxLength\": 40.2000007629395,\n" +
                "      \"MaxLengthType\": \"FL\",\n" +
                "      \"PredatTroph\": null,\n" +
                "      \"PredatseTroph\": null,\n" +
                "      \"PredatRef\": null,\n" +
                "      \"Remarks\": null,\n" +
                "      \"Entered\": 293,\n" +
                "      \"DateEntered\": \"2014-11-11 00:00:00 +0000\",\n" +
                "      \"Modified\": null,\n" +
                "      \"DateModified\": \"2014-11-12 00:00:00 +0000\",\n" +
                "      \"Expert\": null,\n" +
                "      \"DateChecked\": null,\n" +
                "      \"PredId\": 0,\n" +
                "      \"E_Append\": null,\n" +
                "      \"E_DateAppend\": null,\n" +
                "      \"TS\": \"2015-05-11 10:19:19 +0000\"\n" +
                "    }");
        Map<String, String> predatsMap = StudyImporterForFishbase2.parsePredatorPrey(predats, "FB:");

        assertThat(predatsMap.toString(), is("{interactionTypeId=http://purl.obolibrary.org/obo/RO_0002439, interactionTypeName=preysOn, localityId=036, localityName=eastern Australia, sourceLifeStage=recruits/juv., sourceTaxonId=FB:96, studyTitle=FB:REF:97658, targetLifeStage=juv./adults, targetTaxonId=FB:83456}"));
    }

    @Test
    public void parseCountry() throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(COUNTRY_JSON);
        Map<String, String> country = StudyImporterForFishbase2.parseCountry(jsonNode);

        assertThat(country.get(StudyImporterForTSV.DECIMAL_LONGITUDE), is("32"));
        assertThat(country.get(StudyImporterForTSV.DECIMAL_LATITUDE), is("15"));
        assertThat(country.get(StudyImporterForTSV.LOCALITY_ID), is("736"));
    }

    @Test
    public void parseTaxon() throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(SPECIES_JSON);
        Taxon taxon = StudyImporterForFishbase2.parseTaxon(jsonNode, "FB:");

        assertThat(taxon.getName(), is("Oreochromis niloticus"));
        assertThat(taxon.getExternalId(), is("FB:2"));
        assertThat(taxon.getCommonNames(), is("Nile tilapia @en"));

    }

    @Test
    public void parseReference() throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(REFERENCE_JSON);
        Map<String, String> reference = StudyImporterForFishbase2.parseReference(jsonNode, "FB:REF:");
        assertThat(reference.get(StudyImporterForTSV.REFERENCE_ID), is("FB:REF:6160"));
        assertThat(reference.get(StudyImporterForTSV.REFERENCE_CITATION), is("Hickley, P. and R.G. Bailey. 1987. Food and feeding relationships of fish in the Sudd swamps (River Nile, southern Sudan).. J. Fish Biol. 30:147-159."));
        assertThat(reference.get(StudyImporterForTSV.REFERENCE_DOI), is(nullValue()));
    }


    @Test
    public void parseFoodItems() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode foodItem = mapper.readTree(FOOD_ITEM_JSON);

        Map<String, String> foodItemMap = StudyImporterForFishbase2.parseFoodItem(foodItem, "FB:");


        assertThat(foodItemMap.get(StudyImporterForTSV.SOURCE_TAXON_ID), is("FB:2"));
        assertThat(foodItemMap.get(StudyImporterForTSV.SOURCE_LIFE_STAGE), is("juv./adults"));
        assertThat(foodItemMap.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(foodItemMap.get(StudyImporterForTSV.TARGET_LIFE_STAGE), is("n.a./others"));
        assertThat(foodItemMap.get(StudyImporterForTSV.TARGET_TAXON_ID), is(nullValue()));
        assertThat(foodItemMap.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("unidentified > 1 mm organic debris"));
        assertThat(foodItemMap.get(StudyImporterForTSV.LOCALITY_NAME), is("Sudd swamps, River Nile."));
        assertThat(foodItemMap.get(StudyImporterForTSV.REFERENCE_ID), is("FB:REF:6160"));
    }

    @Test
    public void parseFoodItemsSeaLifeBase() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode foodItem = mapper.readTree("{\n" +
                "      \"autoctr\": 4460,\n" +
                "      \"StockCode\": 5964,\n" +
                "      \"SpecCode\": 69400,\n" +
                "      \"Locality\": \"Not specified.\",\n" +
                "      \"C_Code\": null,\n" +
                "      \"FoodsRefNo\": 28499,\n" +
                "      \"FoodI\": \"nekton\",\n" +
                "      \"PreyStage\": \"juv./adults\",\n" +
                "      \"FoodII\": \"finfish\",\n" +
                "      \"FoodIII\": \"bony fish\",\n" +
                "      \"Commoness\": null,\n" +
                "      \"CommonessII\": null,\n" +
                "      \"Foodgroup\": \"Anoplopomatidae\",\n" +
                "      \"Foodname\": \"Anoplopoma fimbria\",\n" +
                "      \"PreySpecCode\": 512,\n" +
                "      \"PreySpecCodeDB\": \"FB\",\n" +
                "      \"AlphaCode\": null,\n" +
                "      \"PreyTroph\": 4.18,\n" +
                "      \"PreySeTroph\": 0.024,\n" +
                "      \"TrophRefNo\": 12626,\n" +
                "      \"PredatorStage\": \"juv./adults\",\n" +
                "      \"Locality2\": null,\n" +
                "      \"Entered\": 1,\n" +
                "      \"Dateentered\": \"2008-07-23 00:00:00 +0000\",\n" +
                "      \"Modified\": null,\n" +
                "      \"Datemodified\": \"2008-07-23 00:00:00 +0000\",\n" +
                "      \"Expert\": null,\n" +
                "      \"Datechecked\": null,\n" +
                "      \"E_Append\": 31,\n" +
                "      \"E_DateAppend\": \"2008-07-23\",\n" +
                "      \"TS\": \"2015-05-11 10:17:16 +0000\"\n" +
                "    }");

        Map<String, String> foodItemMap = StudyImporterForFishbase2.parseFoodItem(foodItem, "SLB:");


        assertThat(foodItemMap.get(StudyImporterForTSV.SOURCE_TAXON_ID), is("SLB:69400"));
        assertThat(foodItemMap.get(StudyImporterForTSV.TARGET_TAXON_ID), is("FB:512"));
        assertThat(foodItemMap.get(StudyImporterForTSV.SOURCE_LIFE_STAGE), is("juv./adults"));
        assertThat(foodItemMap.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(foodItemMap.get(StudyImporterForTSV.TARGET_LIFE_STAGE), is("juv./adults"));
        assertThat(foodItemMap.get(StudyImporterForTSV.TARGET_TAXON_NAME), is(nullValue()));
        assertThat(foodItemMap.get(StudyImporterForTSV.LOCALITY_NAME), is("Not specified."));
        assertThat(foodItemMap.get(StudyImporterForTSV.REFERENCE_ID), is("SLB:REF:28499"));
    }

    private static final String FOOD_ITEM_JSON = "{\n" +
            "      \"autoctr\": 39,\n" +
            "      \"StockCode\": 1,\n" +
            "      \"SpecCode\": 2,\n" +
            "      \"Locality\": \"Sudd swamps, River Nile.\",\n" +
            "      \"C_Code\": \"736\",\n" +
            "      \"FoodsRefNo\": 6160,\n" +
            "      \"FoodI\": \"detritus\",\n" +
            "      \"PreyStage\": \"n.a./others\",\n" +
            "      \"FoodII\": \"detritus\",\n" +
            "      \"FoodIII\": \"debris\",\n" +
            "      \"Commoness\": 12.0,\n" +
            "      \"CommonessII\": \"common (6-20%)\",\n" +
            "      \"Foodgroup\": \"unidentified\",\n" +
            "      \"Foodname\": \"> 1 mm organic debris\",\n" +
            "      \"PreySpecCode\": null,\n" +
            "      \"PreySpecCodeSLB\": null,\n" +
            "      \"AlphaCode\": null,\n" +
            "      \"PreyTroph\": null,\n" +
            "      \"PreySeTroph\": null,\n" +
            "      \"TrophRefNo\": null,\n" +
            "      \"PredatorStage\": \"juv./adults\",\n" +
            "      \"Locality2\": \"Locality: Sudd swamps, River Nile; no. of fish examined=48; size=2.3-35.8 cm SL. Also Refs. 42341, 48499, 58008.\",\n" +
            "      \"Entered\": 34,\n" +
            "      \"Dateentered\": \"1993-10-07 00:00:00 +0000\",\n" +
            "      \"Modified\": 309,\n" +
            "      \"Datemodified\": \"2008-07-09 00:00:00 +0000\",\n" +
            "      \"Expert\": null,\n" +
            "      \"Datechecked\": null,\n" +
            "      \"TS\": null\n" +
            "    }";


    private static final String REFERENCE_JSON = "{\n" +
            "      \"autoctr\": 15563,\n" +
            "      \"RefNo\": 6160,\n" +
            "      \"RMSNo\": 6160,\n" +
            "      \"Author\": \"Hickley, P. and R.G. Bailey\",\n" +
            "      \"Year\": 1987,\n" +
            "      \"Title\": \"Food and feeding relationships of fish in the Sudd swamps (River Nile, southern Sudan).\",\n" +
            "      \"Source\": \"J. Fish Biol. 30:147-159.\",\n" +
            "      \"SourceUnique\": \"J. Fish Biol. 30:147-159.\",\n" +
            "      \"ShortCitation\": \"Hickley et al. 1987\",\n" +
            "      \"FirstAuthor\": \"Hickley\",\n" +
            "      \"Language\": \"English\",\n" +
            "      \"Complete\": null,\n" +
            "      \"RefType\": \"journal article\",\n" +
            "      \"Keywords\": null,\n" +
            "      \"Remarks\": null,\n" +
            "      \"CrossRef1\": null,\n" +
            "      \"CrossRef2\": null,\n" +
            "      \"CrossRef3\": null,\n" +
            "      \"CrossRef4\": null,\n" +
            "      \"CrossRef5\": null,\n" +
            "      \"CAS_REF_NO\": null,\n" +
            "      \"Ecology\": -1,\n" +
            "      \"Ecotoxicology\": 0,\n" +
            "      \"PopDynamics\": 0,\n" +
            "      \"Aquaculture\": 0,\n" +
            "      \"Brains\": 0,\n" +
            "      \"Reproduction\": 0,\n" +
            "      \"Migration\": 0,\n" +
            "      \"Growth\": 0,\n" +
            "      \"Recruitment\": 0,\n" +
            "      \"Vision\": 0,\n" +
            "      \"Ciguatera\": 0,\n" +
            "      \"FryNursery\": 0,\n" +
            "      \"Maturity\": 0,\n" +
            "      \"Distribution\": 0,\n" +
            "      \"Mortality\": 0,\n" +
            "      \"FarmingSystem\": 0,\n" +
            "      \"Eggs\": 0,\n" +
            "      \"Habitats\": 0,\n" +
            "      \"LengthWeight\": 0,\n" +
            "      \"LengthFreq\": 0,\n" +
            "      \"Strains\": 0,\n" +
            "      \"Larvae\": 0,\n" +
            "      \"EnvironmentNode\": 0,\n" +
            "      \"Stocks\": 0,\n" +
            "      \"Genetics\": 0,\n" +
            "      \"Spawning\": 0,\n" +
            "      \"Abundance\": 0,\n" +
            "      \"Catches\": 0,\n" +
            "      \"Electrophoresis\": 0,\n" +
            "      \"SexRatio\": 0,\n" +
            "      \"Activity\": 0,\n" +
            "      \"Effort\": 0,\n" +
            "      \"Diseases\": 0,\n" +
            "      \"Predators\": 0,\n" +
            "      \"Behavior\": 0,\n" +
            "      \"Management\": 0,\n" +
            "      \"Introduction\": 0,\n" +
            "      \"Competitors\": 0,\n" +
            "      \"Food\": -1,\n" +
            "      \"Nomenclature\": 0,\n" +
            "      \"Revision\": 0,\n" +
            "      \"Physiology\": 0,\n" +
            "      \"Morphology\": 0,\n" +
            "      \"Otolith\": 0,\n" +
            "      \"FoodConsum\": 0,\n" +
            "      \"OrigDescr\": 0,\n" +
            "      \"Oxygen\": 0,\n" +
            "      \"MaxLength\": 0,\n" +
            "      \"Diet\": 0,\n" +
            "      \"RawData\": 0,\n" +
            "      \"Speeds\": 0,\n" +
            "      \"MaxWeight\": 0,\n" +
            "      \"Longevity\": 0,\n" +
            "      \"Keys\": 0,\n" +
            "      \"Broodstock\": 0,\n" +
            "      \"EggNursery\": 0,\n" +
            "      \"LarvalNursery\": 0,\n" +
            "      \"Citation\": 0,\n" +
            "      \"Team\": 0,\n" +
            "      \"Aquarium\": 0,\n" +
            "      \"Processing\": 0,\n" +
            "      \"Comname\": 0,\n" +
            "      \"Pictures\": 0,\n" +
            "      \"Tagrecapture\": 0,\n" +
            "      \"Conservation\": 0,\n" +
            "      \"Checklist\": 0,\n" +
            "      \"ISBN\": null,\n" +
            "      \"DOI\": null,\n" +
            "      \"AuthorEmail\": null,\n" +
            "      \"AuthorAdress\": null,\n" +
            "      \"PaperURL\": null,\n" +
            "      \"Used\": \"all species used\",\n" +
            "      \"Entered\": 34,\n" +
            "      \"DateEntered\": \"1993-10-07 00:00:00 +0000\",\n" +
            "      \"Modified\": 967,\n" +
            "      \"DateModified\": \"2003-11-04 00:00:00 +0000\",\n" +
            "      \"Expert\": null,\n" +
            "      \"DateChecked\": null,\n" +
            "      \"TS\": null\n" +
            "    }";


    private static final String SPECIES_JSON = "{\n" +
            "      \"SpecCode\": 2,\n" +
            "      \"Genus\": \"Oreochromis\",\n" +
            "      \"Species\": \"niloticus\",\n" +
            "      \"SpeciesRefNo\": 2,\n" +
            "      \"Author\": \"(Linnaeus, 1758)\",\n" +
            "      \"FBname\": \"Nile tilapia\",\n" +
            "      \"PicPreferredName\": \"Ornil_u6.jpg\",\n" +
            "      \"PicPreferredNameM\": null,\n" +
            "      \"PicPreferredNameF\": null,\n" +
            "      \"PicPreferredNameJ\": null,\n" +
            "      \"FamCode\": 349,\n" +
            "      \"Subfamily\": \"Pseudocrenilabrinae\",\n" +
            "      \"GenCode\": 3459,\n" +
            "      \"SubGenCode\": null,\n" +
            "      \"BodyShapeI\": \"fusiform / normal\",\n" +
            "      \"Source\": \"R\",\n" +
            "      \"AuthorRef\": null,\n" +
            "      \"Remark\": null,\n" +
            "      \"TaxIssue\": 0,\n" +
            "      \"Fresh\": -1,\n" +
            "      \"Brack\": -1,\n" +
            "      \"Saltwater\": 0,\n" +
            "      \"DemersPelag\": \"benthopelagic\",\n" +
            "      \"AnaCat\": \"potamodromous\",\n" +
            "      \"MigratRef\": 51243,\n" +
            "      \"DepthRangeShallow\": 0,\n" +
            "      \"DepthRangeDeep\": 6,\n" +
            "      \"DepthRangeRef\": 32849,\n" +
            "      \"DepthRangeComShallow\": null,\n" +
            "      \"DepthRangeComDeep\": 20,\n" +
            "      \"DepthComRef\": 34290,\n" +
            "      \"LongevityWild\": 9.0,\n" +
            "      \"LongevityWildRef\": 164,\n" +
            "      \"LongevityCaptive\": 12.0,\n" +
            "      \"LongevityCapRef\": 53038,\n" +
            "      \"Vulnerability\": 30.47,\n" +
            "      \"Length\": 60.0,\n" +
            "      \"LTypeMaxM\": \"SL\",\n" +
            "      \"LengthFemale\": null,\n" +
            "      \"LTypeMaxF\": null,\n" +
            "      \"MaxLengthRef\": 4967,\n" +
            "      \"CommonLength\": null,\n" +
            "      \"LTypeComM\": null,\n" +
            "      \"CommonLengthF\": null,\n" +
            "      \"LTypeComF\": null,\n" +
            "      \"CommonLengthRef\": null,\n" +
            "      \"Weight\": 4324.0,\n" +
            "      \"WeightFemale\": null,\n" +
            "      \"MaxWeightRef\": 40637,\n" +
            "      \"Pic\": \"ORNIL_U0\",\n" +
            "      \"PictureFemale\": \"ORNIL_F0\",\n" +
            "      \"LarvaPic\": null,\n" +
            "      \"EggPic\": null,\n" +
            "      \"ImportanceRef\": 4931,\n" +
            "      \"Importance\": \"highly commercial\",\n" +
            "      \"PriceCateg\": \"unknown\",\n" +
            "      \"PriceReliability\": null,\n" +
            "      \"Remarks7\": null,\n" +
            "      \"LandingStatistics\": \"from 100,000 to 500,000\",\n" +
            "      \"Landings\": null,\n" +
            "      \"MainCatchingMethod\": \"seines\",\n" +
            "      \"II\": \"\",\n" +
            "      \"MSeines\": -1,\n" +
            "      \"MGillnets\": -1,\n" +
            "      \"MCastnets\": 0,\n" +
            "      \"MTraps\": -1,\n" +
            "      \"MSpears\": 0,\n" +
            "      \"MTrawls\": -1,\n" +
            "      \"MDredges\": 0,\n" +
            "      \"MLiftnets\": 0,\n" +
            "      \"MHooksLines\": 0,\n" +
            "      \"MOther\": 0,\n" +
            "      \"UsedforAquaculture\": \"commercial\",\n" +
            "      \"LifeCycle\": \"life cycle closed in commercial culture\",\n" +
            "      \"AquacultureRef\": 12108,\n" +
            "      \"UsedasBait\": \"never/rarely\",\n" +
            "      \"BaitRef\": null,\n" +
            "      \"Aquarium\": \"never/rarely\",\n" +
            "      \"AquariumFishII\": \"\",\n" +
            "      \"AquariumRef\": null,\n" +
            "      \"GameFish\": 0,\n" +
            "      \"GameRef\": null,\n" +
            "      \"Dangerous\": \"potential pest\",\n" +
            "      \"DangerousRef\": null,\n" +
            "      \"Electrogenic\": \"no special ability\",\n" +
            "      \"ElectroRef\": null,\n" +
            "      \"Complete\": null,\n" +
            "      \"GoogleImage\": -1,\n" +
            "      \"Comments\": \"Occur in a wide variety of freshwater habitats like rivers, lakes, sewage canals and irrigation channels (Ref. 28714).  Mainly diurnal.  Feed mainly on phytoplankton or benthic algae.    Oviparous (Ref. 205).  Mouthbrooding by females (Ref. 2). Extended temperature range 8-42 °C, natural temperature range 13.5 - 33 °C (Ref. 3).  Marketed fresh and frozen (Ref. 9987).\",\n" +
            "      \"Profile\": null,\n" +
            "      \"PD50\": 0.5,\n" +
            "      \"Emblematic\": 0,\n" +
            "      \"Entered\": 2,\n" +
            "      \"DateEntered\": \"1990-10-17 00:00:00 +0000\",\n" +
            "      \"Modified\": 10,\n" +
            "      \"DateModified\": \"2013-03-30 00:00:00 +0000\",\n" +
            "      \"Expert\": 97,\n" +
            "      \"DateChecked\": \"2003-01-28 00:00:00 +0000\",\n" +
            "      \"TS\": null\n" +
            "    }";

    private static final String COUNTRY_JSON = "{\n" +
            "      \"PAESE\": \"Sudan\",\n" +
            "      \"Note\": \"Sudan 729\",\n" +
            "      \"C_Code\": \"736\",\n" +
            "      \"ABB\": \"SDN\",\n" +
            "      \"ISO\": -1,\n" +
            "      \"AdminCountry\": \"736\",\n" +
            "      \"ISO1Num\": 736,\n" +
            "      \"ISO2Alpha\": \"SD\",\n" +
            "      \"ISO3Alpha\": \"SDN\",\n" +
            "      \"SAUP_Code\": 736,\n" +
            "      \"Used\": -1,\n" +
            "      \"Capital\": \"Khartoum\",\n" +
            "      \"LatDeg\": 15,\n" +
            "      \"LatMin\": 35.0,\n" +
            "      \"NorthSouth\": \"N\",\n" +
            "      \"LongDeg\": 32,\n" +
            "      \"LongMin\": 31.0,\n" +
            "      \"EastWest\": \"E\",\n" +
            "      \"Remark\": \"Independent \",\n" +
            "      \"PolarBoreal\": 0,\n" +
            "      \"Temperate\": 0,\n" +
            "      \"Subtropical\": 0,\n" +
            "      \"Tropical\": -1,\n" +
            "      \"Landlocked\": 0,\n" +
            "      \"GeographicArea\": \"Eastern Central Africa, bordering the Red Sea.\",\n" +
            "      \"Region\": \"Northern Africa\",\n" +
            "      \"Continent\": \"Africa\",\n" +
            "      \"AreaCodeInland\": 1,\n" +
            "      \"FAOAreaInland\": \"Africa - Inland waters\",\n" +
            "      \"AreaCodeMarineI\": 51,\n" +
            "      \"FAOAreaMarineI\": \"Indian Ocean, Western\",\n" +
            "      \"AreaCodeMarineII\": null,\n" +
            "      \"FAOAreaMarineII\": null,\n" +
            "      \"AreaCodeMarineIII\": null,\n" +
            "      \"FAOAreaMarineIII\": null,\n" +
            "      \"AreaCodeMarineIV\": null,\n" +
            "      \"FAOAreaMarineIV\": null,\n" +
            "      \"AreaCodeMarineV\": null,\n" +
            "      \"FAOAreaMarineV\": null,\n" +
            "      \"Population\": 39148,\n" +
            "      \"PopulationYear\": 2004,\n" +
            "      \"PopulationRate\": 2.64000010490417,\n" +
            "      \"PopulationRef\": 53414,\n" +
            "      \"CoastalPopulation\": 10,\n" +
            "      \"CoastPopRef\": 6288,\n" +
            "      \"Area\": 2505800,\n" +
            "      \"Coastline\": 850,\n" +
            "      \"CoastlineRef\": 27871,\n" +
            "      \"ShelfArea\": 22.2999992370605,\n" +
            "      \"ShelfRef\": 6288,\n" +
            "      \"EECarea\": 91.5999984741211,\n" +
            "      \"EEZRef\": 6288,\n" +
            "      \"Language\": \"Arabic\",\n" +
            "      \"Currency\": \"Sudanese pound (Sd)\",\n" +
            "      \"MarineCount\": 333,\n" +
            "      \"MarineFamCount\": 76,\n" +
            "      \"CompleteMarine\": 0,\n" +
            "      \"MarineLit\": null,\n" +
            "      \"MarineFamLit\": null,\n" +
            "      \"CheckMarineRef\": null,\n" +
            "      \"MarineFlag\": 0,\n" +
            "      \"FreshwaterCount\": 133,\n" +
            "      \"FreshFamCount\": 28,\n" +
            "      \"CompleteFresh\": -1,\n" +
            "      \"FreshwaterLit\": null,\n" +
            "      \"FreshFamLit\": null,\n" +
            "      \"CheckFreshRef\": 594,\n" +
            "      \"FreshFlag\": -1,\n" +
            "      \"TotalCount\": 465,\n" +
            "      \"TotalFamCount\": 103,\n" +
            "      \"TotalComplete\": 0,\n" +
            "      \"TotalLit\": null,\n" +
            "      \"TotalFamLit\": null,\n" +
            "      \"TotalRef\": null,\n" +
            "      \"LastUpdate\": \"2010-05-21 00:00:00 +0000\",\n" +
            "      \"MorphCountFresh\": 85,\n" +
            "      \"OccurCountFresh\": 131,\n" +
            "      \"PicCountFresh\": 122,\n" +
            "      \"GrowthCountFresh\": 44,\n" +
            "      \"FoodCountFresh\": 86,\n" +
            "      \"DietCountFresh\": 32,\n" +
            "      \"ReproductionCountFresh\": 64,\n" +
            "      \"SpawningCountFresh\": 37,\n" +
            "      \"MorphCountMarine\": 289,\n" +
            "      \"OccurCountMarine\": 334,\n" +
            "      \"PicCountMarine\": 329,\n" +
            "      \"GrowthCountMarine\": 131,\n" +
            "      \"FoodCountMarine\": 269,\n" +
            "      \"DietCountMarine\": 138,\n" +
            "      \"ReproductionCountMarine\": 204,\n" +
            "      \"SpawningCountMarine\": 109,\n" +
            "      \"LatDegFill\": 14,\n" +
            "      \"LatMinFill\": 30.0,\n" +
            "      \"NorthSouthFill\": \"N\",\n" +
            "      \"LongDegFill\": 32,\n" +
            "      \"LongMinFill\": 31.0,\n" +
            "      \"EastWestFill\": \"E\",\n" +
            "      \"MLatDegFill\": 20,\n" +
            "      \"MLatMinFill\": 30.0,\n" +
            "      \"MNorthSouthFill\": \"N\",\n" +
            "      \"MLongDegFill\": 37,\n" +
            "      \"MLongMinFill\": 30.0,\n" +
            "      \"MEastWestFill\": \"E\",\n" +
            "      \"MLatDegFill2\": null,\n" +
            "      \"MLatMinFill2\": null,\n" +
            "      \"MNorthSouthFill2\": null,\n" +
            "      \"MLongDegFill2\": null,\n" +
            "      \"MLongMinFill2\": null,\n" +
            "      \"MEastWestFill2\": null,\n" +
            "      \"MLatDegFill3\": null,\n" +
            "      \"MLatMinFill3\": null,\n" +
            "      \"MNorthSouthFill3\": null,\n" +
            "      \"MLongDegFill3\": null,\n" +
            "      \"MLongMinFill3\": null,\n" +
            "      \"MEastWestFill3\": null,\n" +
            "      \"MLatDegFill4\": null,\n" +
            "      \"MLatMinFill4\": null,\n" +
            "      \"MNorthSouthFill4\": null,\n" +
            "      \"MLongDegFill4\": null,\n" +
            "      \"MLongMinFill4\": null,\n" +
            "      \"MEastWestFill4\": null,\n" +
            "      \"MLatDegFill5\": null,\n" +
            "      \"MLatMinFill5\": null,\n" +
            "      \"MNorthSouthFill5\": null,\n" +
            "      \"MLongDegFill5\": null,\n" +
            "      \"MLongMinFill5\": null,\n" +
            "      \"MEastWestFill5\": null,\n" +
            "      \"NorthernLatitude\": 22,\n" +
            "      \"NorthernLatitudeNS\": \"N\",\n" +
            "      \"SouthernLatitude\": 9,\n" +
            "      \"SouthernLatitudeNS\": \"N\",\n" +
            "      \"WesternLongitude\": 21,\n" +
            "      \"WesternLongitudeEW\": \"E\",\n" +
            "      \"EasternLongitude\": 39,\n" +
            "      \"EasternLongitudeEW\": \"E\",\n" +
            "      \"CenterLat\": 15.8871414568,\n" +
            "      \"CenterLong\": 30.0899425353,\n" +
            "      \"OtherLanguage\": \"Achooli-Luo, Bari, Beja, Dinka, Lugbara, Nubian, Nuer, Zande \",\n" +
            "      \"Geography\": \"Sudan (with an area of 2,476,800 sq. km.) is a flat country with elevated lands to the east, south and southwest. Vegetation patterns change from tropical forest in the south through semi-tropical savanna to sandy arid hills in the north. Extreme desert conditions prevail in the northwest. The central zone of the country is transected by the Nile Valley with its large swamp depression in the Sudd.\\r\\nThe north of the country has a desertic climate with little rainfall throughout the year. The centre of the country has an unstable climate, with a pronounced rainy season of variable duration. In the south the climate is equatorial, with more or less daily rainfall.\\r\\nSudan is mainly a desertic country; there is, therefore, a very high demand for water. The several reservoirs are for flow retention associated with irrigation areas. The Jonglei Canal will allow about 10% of the present Nile flow to bypass the Sudd Swamps to increase the amount of water available for irrigation downstream.\",\n" +
            "      \"GeographyRef\": 11969,\n" +
            "      \"Hydrography\": \"The freshwater resources of the Sudan have been comprehensively reviewed by CSTR (1982) (Ref. 12127). Dumon (1984) (Ref.  12128) presents the most recent series of papers on limnology and marine biology. This text summarizes some of that data. According to OSRO (1986), (Ref. 12129), nearly 12.90 million hectares are under water, 5% of the total area in the Sudan. Of the available water, less than 50% is used, and the potential water supply is enormous if underground reserves are included. The Nile system is the main feature of the hydrology of Sudan, including the Nile and its tributaries, a number of man-made lakes and the swamps of the Sudd. Non-Nilotic streams of minor importance supply water to 806 hafirs (natural depressions enlarged into small ponds) and 31 dams.\\r\\nLakes: the principal natural lakes in the country are the numerous small floodplain lakes, most important of which is Lake No, which concentrates the major part of the flow from the Nile as it emerges from the Sudd. A few smaller isolated lakes also occur (Welcomme, 1979) (Ref. 12130).\\r\\nRivers, floodplains and swamps: the main river system is that of the Nile. The Albert Nile enters Sudan from Uganda through a narrow gorge in a series of rapids at Nimule. Northward, it becomes known as the Bahr El Jebel which flows into the great swamps of southern Sudan, North of Mangola. The main tributary of the Bahr El Jebel is the Aswa, originating from the Marolo mountain on the Kenyan border. The river widens to several kilometers and divides into the Bahr El Jebel and Bahr El Zeraf in the Sudd Region. There the rivers have numerous channels and follow a serpentine course, to be joined by the Bahr El Ghazal, which drains the southwest part of Sudan, forming the White Nile. South of Malakal, the Sobat River of a catchment area of 224,000 sq. km. consisting of the Baro and Pibar tributaries, joins the While Nile. From here northward the valley is steep sided until it joins the Blue Nile at Khartoum.\\r\\nThe Blue Nile, with a catchment of 325,000 sq. km., originates in Ethiopia and extends 2,000 km until it joins the White Nile to become the River Nile. Within Sudan the Blue Nile is joined by the Dinder and Rabad tributaries. The main Nile passes the Sabaloka Gorge and is joined by the Atbara, a catchment of 100,000 sq. km. and main tributary Sesit. Thereafter, there are two further cataracts before the Nile leaves Sudan at Lake Nubia.\\r\\nReservoirs: a number of major reservoirs have been built or are proposed for the Nile system, mainly for irrigation water storage. Conditions require some form of water storage for the dry period. Natural depressions were enlarged into small ponds of up to 500,000 cu. m., deep; these are known as \\\"hafirs\\\". \\r\\nCoastal lagoons: there are no lagoons of significant size.\",\n" +
            "      \"HydrographyRef\": 11969,\n" +
            "      \"CommentFresh\": \"The following information is to be sought:\\r\\n- Status of knowledge of the freshwater fauna;\\r\\n- Existence of conservation plans;\\r\\n- Information on major aquatic habitats or sites within the country;\\r\\n- Current major threats to species;\\r\\n- Future potential threats to species;\\r\\n- Contact(s) for further information.\",\n" +
            "      \"RefFresh1\": 594,\n" +
            "      \"RefFresh2\": null,\n" +
            "      \"RefFresh3\": null,\n" +
            "      \"FreshPrimary\": 122,\n" +
            "      \"FreshSecondary\": 8,\n" +
            "      \"FreshInto\": 4,\n" +
            "      \"InFisheriesReported\": 0,\n" +
            "      \"InFisheriesPotential\": 59,\n" +
            "      \"InAquaReported\": 1,\n" +
            "      \"InAquaPotential\": 17,\n" +
            "      \"ExportReported\": 14,\n" +
            "      \"ExportPotential\": 26,\n" +
            "      \"SportReported\": 0,\n" +
            "      \"SportPotential\": 18,\n" +
            "      \"EndemicReported\": 4,\n" +
            "      \"EndemicPotential\": 3,\n" +
            "      \"Threatened\": 4,\n" +
            "      \"ProtectedReported\": 0.0,\n" +
            "      \"ProtectedPotential\": 0,\n" +
            "      \"ACP\": -1,\n" +
            "      \"Developed\": 0,\n" +
            "      \"Marine\": 301,\n" +
            "      \"Brackish\": 33,\n" +
            "      \"MarineInto\": 0,\n" +
            "      \"MarineInFisheriesReported\": 3,\n" +
            "      \"MarineInFisheriesPotential\": 240,\n" +
            "      \"MarineInAquaReported\": 0,\n" +
            "      \"MarineInAquaPotential\": 23,\n" +
            "      \"MarineExportReported\": 0,\n" +
            "      \"MarineExportPotential\": 96,\n" +
            "      \"MarineSportReported\": 0,\n" +
            "      \"MarineSportPotential\": 105,\n" +
            "      \"MarineEndemicReported\": 0,\n" +
            "      \"MarineEndemicPotential\": 0,\n" +
            "      \"MarineThreatened\": 11,\n" +
            "      \"MarineProtectedReported\": 0.0,\n" +
            "      \"MarineProtectedPotential\": 2,\n" +
            "      \"NatFishDB\": null,\n" +
            "      \"NatFishDB2\": null,\n" +
            "      \"Factbook\": \"#http://www.cia.gov/cia/publications/factbook/geos/su.html#\",\n" +
            "      \"NatFishAut\": null,\n" +
            "      \"TradeNames\": null,\n" +
            "      \"Entered\": 1,\n" +
            "      \"DateEntered\": \"1993-11-17 00:00:00 +0000\",\n" +
            "      \"Modified\": 949,\n" +
            "      \"DateModified\": \"2014-07-28 00:00:00 +0000\",\n" +
            "      \"Expert\": null,\n" +
            "      \"DateChecked\": null,\n" +
            "      \"Greek_original\": \"??????\",\n" +
            "      \"Greek\": \"&#931;&#959;&#965;&#948;&#940;&#957;\",\n" +
            "      \"RUSSIAN_original\": \"?????\",\n" +
            "      \"RUSSIAN\": \"&#1057;&#1091;&#1076;&#1072;&#1085;\",\n" +
            "      \"FRENCH\": \"Soudan\",\n" +
            "      \"SPANISH\": \"Sudán\",\n" +
            "      \"GERMAN\": \"Sudan \",\n" +
            "      \"DUTCH\": \"Soedan\",\n" +
            "      \"PORTUGUESE\": \"Sudão\",\n" +
            "      \"ITALIAN\": \"Sudan\",\n" +
            "      \"SWEDISH\": \"Sudan \",\n" +
            "      \"TS\": null\n" +
            "    }";

    private static final String PREDATS_JSON = "{\n" +
            "      \"autoctr\": 1,\n" +
            "      \"StockCode\": 1,\n" +
            "      \"SpecCode\": 2,\n" +
            "      \"PredatsRefNo\": 84,\n" +
            "      \"Locality\": \"Not stated.\",\n" +
            "      \"C_Code\": null,\n" +
            "      \"Predatstage\": \"juv./adults\",\n" +
            "      \"PredatorI\": \"finfish\",\n" +
            "      \"PredatorII\": \"bony fish\",\n" +
            "      \"PreyStage\": \"recruits/juv.\",\n" +
            "      \"PredatorGroup\": \"Cichlidae\",\n" +
            "      \"DietP\": null,\n" +
            "      \"StomachContent\": null,\n" +
            "      \"PredatorName\": \"Cichla ocellaris\",\n" +
            "      \"Predatcode\": 457,\n" +
            "      \"AlphaCode\": null,\n" +
            "      \"MaxLength\": null,\n" +
            "      \"MaxLengthType\": null,\n" +
            "      \"PredatTroph\": 3.5,\n" +
            "      \"PredatseTroph\": 0.8,\n" +
            "      \"PredatRef\": null,\n" +
            "      \"Remarks\": null,\n" +
            "      \"Entered\": 3,\n" +
            "      \"DateEntered\": \"1990-10-18 00:00:00 +0000\",\n" +
            "      \"Modified\": 34,\n" +
            "      \"DateModified\": \"2007-05-15 00:00:00 +0000\",\n" +
            "      \"Expert\": null,\n" +
            "      \"DateChecked\": null,\n" +
            "      \"TS\": null\n" +
            "    }";

    private static final String DIET_JSON = "{\n" +
            "      \"DietCode\": 1,\n" +
            "      \"StockCode\": 79,\n" +
            "      \"Speccode\": 69,\n" +
            "      \"DietRefNo\": 9604,\n" +
            "      \"SampleStage\": \"recruits/juv.\",\n" +
            "      \"SampleSize\": 37,\n" +
            "      \"YearStart\": null,\n" +
            "      \"YearEnd\": null,\n" +
            "      \"January\": 0,\n" +
            "      \"February\": 0,\n" +
            "      \"March\": 0,\n" +
            "      \"April\": -1,\n" +
            "      \"May\": -1,\n" +
            "      \"June\": -1,\n" +
            "      \"July\": 0,\n" +
            "      \"August\": 0,\n" +
            "      \"September\": 0,\n" +
            "      \"October\": 0,\n" +
            "      \"November\": 0,\n" +
            "      \"December\": 0,\n" +
            "      \"C_Code\": \"826\",\n" +
            "      \"Locality\": \"Off the west coast of the Isle of Man, 1977-1978\",\n" +
            "      \"E_Code\": 235,\n" +
            "      \"Method\": null,\n" +
            "      \"MethodType\": null,\n" +
            "      \"Remark\": \"Length type derived from given length in Species table.\",\n" +
            "      \"OtherItems\": 0.1,\n" +
            "      \"PercentEmpty\": null,\n" +
            "      \"Troph\": 3.83,\n" +
            "      \"seTroph\": 0.37,\n" +
            "      \"SizeMin\": 30.0,\n" +
            "      \"SizeMax\": 39.0,\n" +
            "      \"SizeType\": \"TL\",\n" +
            "      \"FishLength\": 34.5,\n" +
            "      \"Entered\": 34,\n" +
            "      \"DateEntered\": \"1995-07-29 00:00:00 +0000\",\n" +
            "      \"Modified\": null,\n" +
            "      \"DateModified\": \"2010-12-17 00:00:00 +0000\",\n" +
            "      \"Expert\": null,\n" +
            "      \"DateChecked\": null\n" +
            "    }";
}
