package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class StudyImporterForGoMexSITest {

    @Test
    public void importSingleLine() throws IOException, StudyImporterException {
        final Map<String, String> parsedProperties = new HashMap<String, String>();
        String predOneLine = "REF_ID,PRED_ID,DATA_AGGR_CODE,PRED_SCI_NAME,TOT_PRED_STOM_EXAM,TOT_W_FD,PCT_W_FD,TOT_WO_FD,PCT_WO_FD,PRED_DEN,SEX,SEX_RATIO,LIFE_HIST_STAGE,LEN_TYPE,MIN_LEN,MAX_LEN,MN_LEN,MIN_AGE,MAX_AGE,MN_AGE,MIN_WT,MAX_WT,MN_WT,TOT_WT,BIOMASS_DEN,STOM_PCT_FULL,STOM_COND_INDEX,TOT_FD_BIOMASS,TOT_FD_VOL,TOT_FD_CAT,TOT_FD_CAT_W_CNT_DATA,TOT_FD_ITEMS_IN_CAT_W_CNT_DATA,PREDATOR_NOTES,POP _TOT_PRED,POP_PRED_DEN,POP_SEX_RATIO,POP_LIFE_HIST_STAGE,POP_LEN_TYPE,POP_MIN_LEN,POP_MAX_LEN,POP_MN_LEN,POP_MIN_AGE,POP_MAX_AGE,POP_MN_AGE,POP_MIN_WT,POP_MAX_WT,POP_MN_WT,POP_TOT_WT,POP_BIOMASS_DEN\n" +
                "16r,Cchr.1,complete,Chloroscombrus chrysurus,66,61,0.9242,5,0.0758,NA,NA,NA,adult,SL,101,150,125.5,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,8.3,17,0,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA";
        StudyImporterForGoMexSI.parseSpecimen("test.txt", "PRED_SCI_NAME", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                parsedProperties.putAll(properties);
            }
        }, new LabeledCSVParser(new CSVParser(new StringReader(predOneLine))));

        assertThat(parsedProperties.get("name"), is("Chloroscombrus chrysurus"));
        assertThat(parsedProperties.get("GOMEXSI:PRED_SCI_NAME"), is("Chloroscombrus chrysurus"));
    }

    @Test
    public void importSinglePrey() throws IOException, StudyImporterException {
        final Map<String, String> parsedProperties = new HashMap<String, String>();
        String predOneLine = "REF_ID,PRED_ID,SOURCE_PREY_NAME,DATABASE_PREY_NAME,PHYSIOLOG_STATE,SED_ORIGIN,PREY_PARTS,LIFE_HIST_STAGE,COND_INDEX,SEX,SEX_RATIO,LEN_TYPE,MIN_LEN,MAX_LEN,MN_LEN,BIOMASS,PCT_BIOMASS,N_CONS,PCT_N_CONS,VOL_CONS,PCT_VOL_CONS,FREQ_OCC,PCT_FREQ_OCC,IRI,PCT_IRI,IRIa,IIR,E,PREY_NOTES,FB_FOOD_I,FB_FOOD_II,FB_FOOD_III,FB_STAGE\n" +
                "16r,Cchr.1,Crustacea,Crustacea,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,1.245,0.15,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA";
        StudyImporterForGoMexSI.parseSpecimen("test.txt", "DATABASE_PREY_NAME", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                parsedProperties.putAll(properties);
            }
        }, new LabeledCSVParser(new CSVParser(new StringReader(predOneLine))));

        assertThat(parsedProperties.get("name"), is("Crustacea"));
        assertThat(parsedProperties.get("GOMEXSI:SOURCE_PREY_NAME"), is("Crustacea"));
    }

    @Test
    public void importReferences() throws IOException, StudyImporterException {
        String someReferences = "REF_ID,GAME_ID,DATA_SOURCE_TYPE,THESIS_TYPE,REF_TAG,AUTH_L_NAME,AUTH_SUFFIX,AUTH_F_NAME,AUTH_M_INIT,AUTH_EMAIL,AUTH_PHONE,AUTH_COUNTRY_CODE,AUTH_NOTES,YEAR_PUB,TITLE_REF,EDITOR_L_NAME,EDITOR_F_NAME,EDITOR_M_INIT,SOURCE_NAME,VOL,NUM,START_PAGE,END_PAGE,DOI,PUB_NAME,PUB_CITY,PUB_STATE,PUB_COUNTRY,UNIV_NAME,UNIV_CITY,UNIV_STATE,UNIV_COUNTRY,NUM_SPECIES_EXAM,REF_NOTES\n" +
                "16r,3017,T,PhD  ,Rogers 1977,Rogers,Jr.,Robert,M,NA,NA,NA,NA,1977,Trophic interrelationships of selected fishes on the continental shelf of the northern Gulf of Mexico,NA,NA,NA,NA,NA,NA,1,244,,NA,NA,NA,NA,Texas A&M University,College Station,TX,United States,26,NA\n" +
                "24d,2689,PL,NA,Divita et al 1983,Divita,NA,Regina,NA,NA,NA,NA,NA,1983,\"Foods of coastal fishes during brown shrimp Penaeus aztecus, migration from Texas estuaries (June - July 1981). \",NA,NA,NA,Fisheries Bulletin,81,2,396,404,,NA,NA,NA,NA,NA,NA,NA,NA,81,NA\n" +
                "24d,2689,PL,NA,Divita et al 1983,Creel,NA,Mischelle,NA,NA,NA,NA,NA,1983,\"Foods of coastal fishes during brown shrimp Penaeus aztecus, migration from Texas estuaries (June - July 1981). \",NA,NA,NA,Fisheries Bulletin,81,2,396,404,,NA,NA,NA,NA,NA,NA,NA,NA,81,NA\n" +
                "24d,2689,PL,NA,Divita et al 1983,Sheridan,NA,Peter,F,NA,NA,NA,NA,1983,\"Foods of coastal fishes during brown shrimp Penaeus aztecus, migration from Texas estuaries (June - July 1981). \",NA,NA,NA,Fisheries Bulletin,81,2,396,404,,NA,NA,NA,NA,NA,NA,NA,NA,81,NA\n" +
                "17g,2746,PL,NA,\"Giménez et al, 2001\",Moreno,NA,Víctor,NA,NA,NA,NA,NA,2001,Aspectos d    e la conducta alimentaria del mero (Epinephelus morio) del Banco de Campeche,NA,NA,N    A,Ciencia Pesquera,NA,14,165,170,NA,INAPESCA,Del Benito,Juárez,MX,NA,NA,NA,NA,1,NA\n" +
                "17b,2550,GR,NA,Beaumariage 1973,Beaumariage,NA,Dale,S,NA,NA,NA,NA,1973,\"Age, growth, and reproduction of king mackerel, Scomberomorus cavalla, in Florida.  \",NA,NA,NA,Florida Marine Research Publications,NA,1,1,45,,\"Marine Research Laboratory, Florida Department of Natural Resources\",St. Petersburg,FL,US,NA,NA,NA,NA,1,NA";

        Map<String, String> contributorMap = StudyImporterForGoMexSI.collectContributors("bla", new LabeledCSVParser(new CSVParser(new StringReader(someReferences))));

        assertThat(contributorMap.get("17g"), is("Víctor Moreno"));
        assertThat(contributorMap.get("16r"), is("Robert Rogers"));
        assertThat(contributorMap.get("24d"), is("Regina Divita, Mischelle Creel, Peter Sheridan"));
        assertThat(contributorMap.get("17b"), is("Dale Beaumariage"));
    }

}