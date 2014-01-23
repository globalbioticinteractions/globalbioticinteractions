package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class StudyImporterForRaymondTest extends GraphDBTestCase {

    @Test
    public void importStudy() throws StudyImporterException {
        StudyImporter importer = new StudyImporterForRaymond(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();
    }

    @Test
    public void importPartialStudy() throws IOException {
        StudyImporterForRaymond importer = new StudyImporterForRaymond(new ParserFactoryImpl(), nodeFactory);
        LabeledCSVParser dietParser = new LabeledCSVParser(new CSVParser(new StringReader(firstFewLinesOfDiet())));
        LabeledCSVParser sourcesParser = new LabeledCSVParser(new CSVParser(new StringReader(firstFewLinesOfSourcesAlteredToFitDietDataSample())));
        importer.importData(sourcesParser, dietParser);
    }

    private String firstFewLinesOfDiet() {
        return "LINK_ID,LOCATION,WEST,EAST,SOUTH,NORTH,OBSERVATION_DATE_START,OBSERVATION_DATE_END,ALTITUDE_MIN,ALTITUDE_MAX,DEPTH_MIN,DEPTH_MAX,PREDATOR_NAME,PREDATOR_NAME_ORIGINAL,PREDATOR_COMMON_NAME,PREDATOR_APHIA_ID,PREDATOR_BREEDING_STAGE,PREDATOR_LIFE_STAGE,PREDATOR_SEX,PREDATOR_TOTAL_COUNT,PREDATOR_SAMPLE_COUNT,PREDATOR_SAMPLE_ID,PREDATOR_SIZE_MIN,PREDATOR_SIZE_MAX,PREDATOR_SIZE_MEAN,PREDATOR_SIZE_SD,PREDATOR_SIZE_UNITS,PREDATOR_SIZE_NOTES,PREDATOR_MASS_MIN,PREDATOR_MASS_MAX,PREDATOR_MASS_MEAN,PREDATOR_MASS_SD,PREDATOR_MASS_UNITS,PREDATOR_MASS_NOTES,PREY_NAME,PREY_NAME_ORIGINAL,PREY_COMMON_NAME,PREY_APHIA_ID,PREY_IS_AGGREGATE,PREY_SAMPLE_COUNT,PREY_LIFE_STAGE,PREY_SIZE_MIN,PREY_SIZE_MAX,PREY_SIZE_MEAN,PREY_SIZE_SD,PREY_SIZE_UNITS,PREY_SIZE_NOTES,PREY_MASS_MIN,PREY_MASS_MAX,PREY_MASS_MEAN,PREY_MASS_SD,PREY_MASS_UNITS,PREY_MASS_NOTES,FRACTION_DIET_BY_WEIGHT,FRACTION_DIET_BY_PREY_ITEMS,FRACTION_OCCURRENCE,QUALITATIVE_DIETARY_IMPORTANCE,CONSUMPTION_RATE_MIN,CONSUMPTION_RATE_MAX,CONSUMPTION_RATE_MEAN,CONSUMPTION_RATE_SD,CONSUMPTION_RATE_UNITS,CONSUMPTION_RATE_NOTES,IDENTIFICATION_METHOD,IS_SECONDARY_DATA,QUALITY_FLAG,SOURCE_ID,NOTES,LAST_MODIFIED\n" +
                "1,Croker Passage,-61.66666667,-61.66666667,-63.95,-63.95,8/11/1992,8/11/1992,,,0,1020,Eukrohnia hamata,Eukrohnia hamata,arrow worm,105416,,,unknown,802,802,8,,,,,,,,,,,,,Metridia gerlachei,Metridia gerlachei,,344689,N,8,,,,,,,,,,,,,,,0.12,,,,,,,,,gut content,N,G,17,,13/01/2011 19:55\n" +
                "2,Croker Passage,-61.66666667,-61.66666667,-63.95,-63.95,8/11/1992,8/11/1992,,,0,1020,Eukrohnia hamata,Eukrohnia hamata,arrow worm,105416,,,unknown,802,802,8,,,,,,,,,,,,,Oncaea sp.,Oncaea sp.,,128690,N,7,,,,,,,,,,,,,,,0.1,,,,,,,,,gut content,N,G,17,,13/01/2011 19:55\n" +
                "3,Croker Passage,-61.66666667,-61.66666667,-63.95,-63.95,8/11/1992,8/11/1992,,,0,1020,Eukrohnia hamata,Eukrohnia hamata,arrow worm,105416,,,unknown,802,802,8,,,,,,,,,,,,,Microcalanus pygmaeus,Microcalanus pygmaeus,,104513,N,23,,,,,,,,,,,,,,,0.34,,,,,,,,,gut content,N,G,17,,13/01/2011 19:55\n" +
                "4,Croker Passage,-61.66666667,-61.66666667,-63.95,-63.95,8/11/1992,8/11/1992,,,0,1020,Eukrohnia hamata,Eukrohnia hamata,arrow worm,105416,,,unknown,802,802,8,,,,,,,,,,,,,Oithona sp.,Oithona sp.,,106485,N,1,,,,,,,,,,,,,,,0.01,,,,,,,,,gut content,N,G,17,,13/01/2011 19:55";
    }

    private String firstFewLinesOfSourcesAlteredToFitDietDataSample() {
        return "SOURCE_ID,DETAILS,NOTES,DATE_CREATED\n" +
                "2,\"Hopkins, T.L. (1985) Food web of an Antarctic midwater ecosystem. Marine Biology 89:197-212\",\"The diets of 93 species of plankton and micronekton taken in the upper 1000m of Croker Passage (Gerlache Strait) in the austral fall, 1983, were examined and the principal features of the food web were characterized.\",14/01/2008 9:15\n" +
                "3,\"Hopkins, T.L., Ainley, D.G., Torres, J.J., and Lancraft, T.M. (1993) Trophic structure in open waters of the marginal ice zone in the Scotia-Weddell confluence region during spring (1983). Polar Biology 13:389-397\",\"The structure of the food web was investigated in open waters adjacent to the marginal ice zone in the southern Scotia Sea in spring 1983. ... Most zooplankton were omnivorous, feeding on phytoplankton, protozoans, and in some cases, small metazoans (copepods). Only two species were found to be exclusively herbivorous: Calanoides acutus and Rhincalanus gigas. Micronekton were carnivores with copepods being the dominant prey in all their diets. The midwater fish Electrona antarctica was the dominant food item in seven of the nine seabird speeies examined. Cephalopods, midwater decapod shrimps and carrion were also important in the diets of a few seabird species. Comparison (eluster analysis) of diets in spring with other seasons (winter, fall) indicated that over half the speeies examined (18 of 31) had similar diets in all seasons tested.\",14/01/2008 9:16\n" +
                "4,\"Hopkins, T.L. (1987) Midwater food web in McMurdo Sound, Ross Sea, Antarctica. Marine Biology 96:93-106\",\"The trophic structure of the midwater ecosystem of McMurdo Sound, Ross Sea, Antarctica in February, 1983 was examined through diet analysis of 35 species of zooplankton and micronekton. Ten feeding groups were suggested through cluster analysis. The two largest clusters consisted of small-particle grazers and omnivore generalists; the eight remaining clusters were of carnivores specializing on one or several types of metazoan prey. Diet composition often shifted with ontogeny and though exceptions occurred, diet diversity usually increased either during early growth or throughout development. Comparison with a krill-dominated area along the Antarctic Penisula (Croker Passage) indicated that species common to the two areas occupied approximately the same trophic position. Biomass in McMurdo Sound was much lower than in Croker Passage and large-sized particle grazers such as krill and salps were trophically less dominant in McMurdo Sound. [truncated]\",14/01/2008 9:17";
    }
}
