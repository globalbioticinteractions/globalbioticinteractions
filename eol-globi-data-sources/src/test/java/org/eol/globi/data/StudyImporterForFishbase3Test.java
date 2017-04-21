package org.eol.globi.data;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForFishbase3Test {

    private Map<String, Map<String, String>> countries;
    private Map<String, Map<String, String>> speciesMap;
    private Map<String, Map<String, String>> references;

    @Before
    public void importDependencies() throws StudyImporterException {
        references = new HashMap<>();
        InputStream resourceAsStream2 = getClass().getResourceAsStream("refrens_fishbase_first100.tsv");
        StudyImporterForFishbase3.importReferences(references, resourceAsStream2, "FB");

        speciesMap = new HashMap<>();
        InputStream resourceAsStream1 = getClass().getResourceAsStream("species_fishbase_first100.tsv");
        StudyImporterForFishbase3.importSpecies(speciesMap, resourceAsStream1, "FB");
        InputStream resourceAsStream3 = getClass().getResourceAsStream("species_sealifebase_first100.tsv");
        StudyImporterForFishbase3.importSpecies(speciesMap, resourceAsStream3, "SLB");

        countries = new HashMap<>();
        InputStream resourceAsStream = getClass().getResourceAsStream("countref_fishbase_first100.tsv");
        StudyImporterForFishbase3.importCountries(countries, resourceAsStream);
    }

    @Test
    public void parseFoodItems() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("fooditems_fishbase_first100.tsv");

        StudyImporterForFishbase3.importFoodItemsByFoodName(links::add, is, speciesMap, references, countries, "FB");

        Map<String, String> firstItem = links.get(0);

        assertThat(firstItem.get("referenceCitation"), Is.is("Hickley, P. and R.G. Bailey. 1987. Food and feeding relationships of fish in the Sudd swamps (River Nile, southern Sudan). J. Fish Biol. 30:147-159."));
        assertThat(firstItem.get("decimalLongitude"), Is.is("30.0899425353"));
        assertThat(firstItem.get("decimalLatitude"), Is.is("15.8871414568"));
        assertThat(firstItem.get("referenceUrl"), Is.is("http://fishbase.org/references/FBRefSummary.php?id=6160"));
        assertThat(firstItem.get("localityName"), Is.is("Sudan|Sudd swamps, River Nile."));
        assertThat(firstItem.get("localityId"), Is.is("FB_COUNTRY:736|"));
        assertThat(firstItem.get("sourceTaxonId"), Is.is("FBC:FB:SPECCODE:2"));
        assertThat(firstItem.get("sourceTaxonName"), Is.is("Oreochromis niloticus"));
        assertThat(firstItem.get("sourceLifeStage"), Is.is("juv./adults"));
        assertThat(firstItem.get("targetTaxonName"), Is.is("< 1 mm organic debris"));
        assertThat(firstItem.get("targetLifeStage"), Is.is("n.a./others"));
        assertThat(firstItem.get("studyTitle"), Is.is("FB_REF:6160"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), Is.is("eats"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002470"));
    }

    @Test
    public void parseFoodItemII() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("fooditems_fishbase_first100.tsv");

        StudyImporterForFishbase3.importFoodItemsByFoodII(links::add, is, speciesMap, references, countries, "FB");

        Map<String, String> firstItem = links.get(0);

        assertThat(firstItem.get("referenceCitation"), Is.is("Hickley, P. and R.G. Bailey. 1987. Food and feeding relationships of fish in the Sudd swamps (River Nile, southern Sudan). J. Fish Biol. 30:147-159."));
        assertThat(firstItem.get("decimalLongitude"), Is.is("30.0899425353"));
        assertThat(firstItem.get("decimalLatitude"), Is.is("15.8871414568"));
        assertThat(firstItem.get("referenceUrl"), Is.is("http://fishbase.org/references/FBRefSummary.php?id=6160"));
        assertThat(firstItem.get("localityName"), Is.is("Sudan|Sudd swamps, River Nile."));
        assertThat(firstItem.get("localityId"), Is.is("FB_COUNTRY:736|"));
        assertThat(firstItem.get("sourceTaxonId"), Is.is("FBC:FB:SPECCODE:2"));
        assertThat(firstItem.get("sourceTaxonName"), Is.is("Oreochromis niloticus"));
        assertThat(firstItem.get("sourceLifeStage"), Is.is("juv./adults"));
        assertThat(firstItem.get("targetTaxonName"), Is.is("detritus"));
        assertThat(firstItem.get("studyTitle"), Is.is("FB_REF:6160"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), Is.is("eats"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002470"));
    }


    @Test
    public void parsePredats() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("predats_fishbase_first100.tsv");

        StudyImporterForFishbase3.importPredators(links::add, is, speciesMap, references, countries, "FB");

        Map<String, String> firstItem = links.get(0);

        assertThat(firstItem.get("referenceCitation"), Is.is("Guerrero, R.D. III. 1982. Control of tilapia reproduction. p. 309-316. In R.S.V. Pullin and R.H. Lowe-McConnell (eds.) The biology and culture of tilapias. ICLARM Conf. Proc. 7. 432 p."));
        assertThat(firstItem.get("decimalLongitude"), Is.is(nullValue()));
        assertThat(firstItem.get("decimalLatitude"), Is.is(nullValue()));
        assertThat(firstItem.get("referenceUrl"), Is.is("http://fishbase.org/references/FBRefSummary.php?id=84"));
        assertThat(firstItem.get("localityName"), Is.is(nullValue()));
        assertThat(firstItem.get("localityId"), Is.is(nullValue()));
        assertThat(firstItem.get("sourceTaxonId"), Is.is("FBC:FB:SPECCODE:457"));
        assertThat(firstItem.get("sourceTaxonName"), Is.is("Cichla ocellaris"));
        assertThat(firstItem.get("sourceLifeStage"), Is.is("juv./adults"));
        assertThat(firstItem.get("targetTaxonId"), Is.is("FBC:FB:SPECCODE:2"));
        assertThat(firstItem.get("targetTaxonName"), Is.is("Oreochromis niloticus"));
        assertThat(firstItem.get("targetLifeStage"), Is.is("recruits/juv."));
        assertThat(firstItem.get("studyTitle"), Is.is("FB_REF:84"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), Is.is("preysOn"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002439"));

        Map<String, String> secondItem = links.get(1);

        assertThat(secondItem.get("decimalLongitude"), Is.is("39.6356930625"));
        assertThat(secondItem.get("decimalLatitude"), Is.is("8.62180446746"));
        assertThat(secondItem.get("localityName"), Is.is("Ethiopia|Lake Awassa"));
        assertThat(secondItem.get("localityId"), Is.is("FB_COUNTRY:230|"));
    }

    @Test
    public void parseDiet() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("diet_fishbase_first100.tsv");

        StudyImporterForFishbase3.importDiet(links::add, is, speciesMap, references, countries, "FB");

        Map<String, String> firstItem = links.get(0);

        assertThat(firstItem.get("referenceCitation"), Is.is("Armstrong, M J. 1982. The predator-prey relationships of Irish Sea poor-cod (<i>Trisopterus minutus</i> L.), pouting (<i>Trisopterus luscus</i> L), and cod (<i>Gadus morhua</i> L.). J. Cons. Int. Explor. Mer. 40(2):135-152."));
        assertThat(firstItem.get("decimalLongitude"), Is.is("-1.69182449421"));
        assertThat(firstItem.get("decimalLatitude"), Is.is("52.8763053517"));
        assertThat(firstItem.get("referenceUrl"), Is.is("http://fishbase.org/references/FBRefSummary.php?id=9604"));
        assertThat(firstItem.get("localityName"), Is.is("UK|Off the west coast of the Isle of Man, 1977-1978"));
        assertThat(firstItem.get("localityId"), Is.is("FB_COUNTRY:826|"));
        assertThat(firstItem.get("sourceTaxonId"), Is.is("FBC:FB:SPECCODE:69"));
        assertThat(firstItem.get("sourceTaxonName"), Is.is("Gadus morhua"));
        assertThat(firstItem.get("sourceLifeStage"), Is.is("recruits/juv."));
        assertThat(firstItem.get("targetTaxonId"), Is.is(nullValue()));
        assertThat(firstItem.get("targetTaxonName"), Is.is("gastropods"));
        assertThat(firstItem.get("targetLifeStage"), Is.is("n.a./others"));
        assertThat(firstItem.get("studyTitle"), Is.is("FB_REF:9604"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), Is.is("eats"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(firstItem.get("localityName"), Is.is("UK|Off the west coast of the Isle of Man, 1977-1978"));

        Map<String, String> secondItem = links.get(1);

        assertThat(secondItem.get("localityName"), Is.is("UK|Off the west coast of the Isle of Man, 1977-1978"));
        assertThat(secondItem.get("localityId"), Is.is("FB_COUNTRY:826|"));
    }

}