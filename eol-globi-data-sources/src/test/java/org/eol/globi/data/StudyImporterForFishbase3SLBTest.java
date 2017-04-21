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

public class StudyImporterForFishbase3SLBTest {

    private Map<String, Map<String, String>> countries;
    private Map<String, Map<String, String>> speciesMap;
    private Map<String, Map<String, String>> references;

    @Before
    public void importDependencies() throws StudyImporterException {
        references = new HashMap<>();
        InputStream resourceAsStream2 = getClass().getResourceAsStream("refrens_sealifebase_first100.tsv");
        StudyImporterForFishbase3.importReferences(references, resourceAsStream2, "SLB");

        speciesMap = new HashMap<>();
        InputStream resourceAsStream1 = getClass().getResourceAsStream("species_sealifebase_first100.tsv");
        StudyImporterForFishbase3.importSpecies(speciesMap, resourceAsStream1, "SLB");
        InputStream resourceAsStream3 = getClass().getResourceAsStream("species_fishbase_first100.tsv");
        StudyImporterForFishbase3.importSpecies(speciesMap, resourceAsStream3, "FB");

        countries = new HashMap<>();
        InputStream resourceAsStream = getClass().getResourceAsStream("countref_sealifebase_first100.tsv");
        StudyImporterForFishbase3.importCountries(countries, resourceAsStream);
    }

    @Test
    public void parseFoodItems() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("fooditems_sealifebase_first100.tsv");

        StudyImporterForFishbase3.importFoodItemsByFoodName(links::add, is, speciesMap, references, countries, "SLB");

        Map<String, String> firstItem = links.get(0);

        assertThat(firstItem.get("referenceCitation"), Is.is("Pechenik, J.A. 2005. Biology of the Invertebrates. Fifth Edition. Mc-Graw-Hill. New York, USA. 590 p."));
        assertThat(firstItem.get("decimalLongitude"), Is.is(nullValue()));
        assertThat(firstItem.get("decimalLatitude"), Is.is(nullValue()));
        assertThat(firstItem.get("referenceUrl"), Is.is("http://sealifebase.org/references/FBRefSummary.php?id=53"));
        assertThat(firstItem.get("localityName"), Is.is(nullValue()));
        assertThat(firstItem.get("localityId"), Is.is(nullValue()));
        assertThat(firstItem.get("sourceTaxonId"), Is.is("FBC:SLB:SPECCODE:23"));
        assertThat(firstItem.get("sourceTaxonName"), Is.is("Trichoplax adhaerens"));
        assertThat(firstItem.get("sourceLifeStage"), Is.is("juv./adults"));
        assertThat(firstItem.get("targetTaxonName"), Is.is("dead animals"));
        assertThat(firstItem.get("targetLifeStage"), Is.is("n.a./others"));
        assertThat(firstItem.get("studyTitle"), Is.is("SLB_REF:53"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), Is.is("eats"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002470"));
    }


    @Test
    public void parsePredats() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("predats_sealifebase_first100.tsv");

        StudyImporterForFishbase3.importPredators(links::add, is, speciesMap, references, countries, "SLB");

        Map<String, String> firstItem = links.get(0);

        assertThat(firstItem.get("referenceCitation"), Is.is("Griffiths, S.P., P.M. Kuhnert, G.F. Fry and F.J. Manson. 2009. Temporal and size-related variation in the diet, consumption rate, and daily ration of mackerel tuna (<i>Euthynnus affinis</i>) in neritic waters of eastern Australia. ICES Journal of Marine Science: Journal du Conseil 66(4): 720-733."));
        assertThat(firstItem.get("decimalLongitude"), Is.is("134.486759137"));
        assertThat(firstItem.get("decimalLatitude"), Is.is("-25.7325344275"));
        assertThat(firstItem.get("referenceUrl"), Is.is("http://sealifebase.org/references/FBRefSummary.php?id=97658"));
        assertThat(firstItem.get("localityName"), Is.is("Australia|eastern Australia"));
        assertThat(firstItem.get("localityId"), Is.is("SLB_COUNTRY:036|"));
        assertThat(firstItem.get("sourceTaxonId"), Is.is("FBC:FB:SPECCODE:96"));
        assertThat(firstItem.get("sourceTaxonName"), Is.is("Euthynnus affinis"));
        assertThat(firstItem.get("sourceLifeStage"), Is.is("recruits/juv."));
        assertThat(firstItem.get("targetTaxonId"), Is.is("FBC:SLB:SPECCODE:83456"));
        assertThat(firstItem.get("targetTaxonName"), Is.is("Odontodactylus cultrifer"));
        assertThat(firstItem.get("targetLifeStage"), Is.is("juv./adults"));
        assertThat(firstItem.get("studyTitle"), Is.is("SLB_REF:97658"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), Is.is("preysOn"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002439"));

    }

    @Test
    public void parseDiet() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("diet_sealifebase_first100.tsv");

        StudyImporterForFishbase3.importDiet(links::add, is, speciesMap, references, countries, "SLB");

        Map<String, String> firstItem = links.get(0);

        assertThat(firstItem.get("referenceCitation"), Is.is("de Lestang, S., I.C. Platell and M.E. Potter. 2000. Dietary composition of the blue swimmer crab <i>Portunus pelagicus</i>L. Does it vary with body size and shell state and between estuaries?. Journal of Experimental Marine Biology and Ecology 246:241-257."));
        assertThat(firstItem.get("decimalLongitude"), Is.is("134.486759137"));
        assertThat(firstItem.get("decimalLatitude"), Is.is("-25.7325344275"));
        assertThat(firstItem.get("referenceUrl"), Is.is("http://sealifebase.org/references/FBRefSummary.php?id=8747"));
        assertThat(firstItem.get("localityName"), Is.is("Australia|Peel-Harvey (32°40'South, 115°40'East), Australia"));
        assertThat(firstItem.get("localityId"), Is.is("SLB_COUNTRY:036|"));
        assertThat(firstItem.get("sourceTaxonId"), Is.is("FBC:SLB:SPECCODE:9329"));
        assertThat(firstItem.get("sourceTaxonName"), Is.is("Portunus pelagicus"));
        assertThat(firstItem.get("sourceLifeStage"), Is.is("juv./adults"));
        assertThat(firstItem.get("targetTaxonId"), Is.is(nullValue()));
        assertThat(firstItem.get("targetTaxonName"), Is.is("Gammarid amphipods"));
        assertThat(firstItem.get("targetLifeStage"), Is.is("juv./adults"));
        assertThat(firstItem.get("studyTitle"), Is.is("SLB_REF:8747"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), Is.is("eats"));
        assertThat(firstItem.get(StudyImporterForTSV.INTERACTION_TYPE_ID), Is.is("http://purl.obolibrary.org/obo/RO_0002470"));

        Map<String, String> secondItem = links.get(1);

        assertThat(secondItem.get("targetTaxonId"), Is.is("FBC:SLB:SPECCODE:57331"));
        assertThat(secondItem.get("targetTaxonName"), Is.is("Taningia danae"));
    }

}