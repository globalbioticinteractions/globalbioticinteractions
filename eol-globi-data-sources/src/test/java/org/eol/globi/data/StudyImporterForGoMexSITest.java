package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
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
    public void importSinglePreyWithPCTValues() throws IOException, StudyImporterException {
        final Map<String, String> parsedProperties = new HashMap<String, String>();
        String predOneLine = "REF_ID,PRED_ID,SOURCE_PREY_NAME,DATABASE_PREY_NAME,PHYSIOLOG_STATE,SED_ORIGIN,PREY_PARTS,LIFE_HIST_STAGE,COND_INDEX,SEX,SEX_RATIO,LEN_TYPE,MIN_LEN,MAX_LEN,MN_LEN,BIOMASS,PCT_BIOMASS,N_CONS,PCT_N_CONS,VOL_CONS,PCT_VOL_CONS,FREQ_OCC,PCT_FREQ_OCC,IRI,PCT_IRI,IRIa,IIR,E,PREY_NOTES,FB_FOOD_I,FB_FOOD_II,FB_FOOD_III,FB_STAGE\n" +
                "16r,Cchr.1,Crustacea,Crustacea,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,0.6,0.5,0.4,0.3,0.2,0.1,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA";
        StudyImporterForGoMexSI.parseSpecimen("test.txt", "DATABASE_PREY_NAME", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                parsedProperties.putAll(properties);
            }
        }, new LabeledCSVParser(new CSVParser(new StringReader(predOneLine))));

        assertThat(parsedProperties.get("name"), is("Crustacea"));
        assertThat(parsedProperties.get("GOMEXSI:SOURCE_PREY_NAME"), is("Crustacea"));
        assertThat(parsedProperties.get("GOMEXSI:N_CONS"), is("0.6"));
        assertThat(parsedProperties.get("GOMEXSI:PCT_N_CONS"), is("0.5"));
        assertThat(parsedProperties.get("GOMEXSI:VOL_CONS"), is("0.4"));
        assertThat(parsedProperties.get("GOMEXSI:PCT_VOL_CONS"), is("0.3"));
        assertThat(parsedProperties.get("GOMEXSI:FREQ_OCC"), is("0.2"));
        assertThat(parsedProperties.get("GOMEXSI:PCT_FREQ_OCC"), is("0.1"));

        assertThat(parsedProperties.get(Specimen.TOTAL_COUNT), is("0.6"));
        assertThat(parsedProperties.get(Specimen.TOTAL_COUNT_PERCENT), is("0.5"));
        assertThat(parsedProperties.get(Specimen.TOTAL_VOLUME_IN_ML), is("0.4"));
        assertThat(parsedProperties.get(Specimen.TOTAL_VOLUME_PERCENT), is("0.3"));
        assertThat(parsedProperties.get(Specimen.FREQUENCY_OF_OCCURRENCE), is("0.2"));
        assertThat(parsedProperties.get(Specimen.FREQUENCY_OF_OCCURRENCE_PERCENT), is("0.1"));
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

    @Test
    public void polyCoordToWKT() {
        assertThat(StudyImporterForGoMexSI.polyCoordsToWKT("((-92.6729107838999,29.3941413332999),(-92.5604838626999,29.2066775354))")
                , is("POLYGON((-92.6729107838999 29.3941413332999,-92.5604838626999 29.2066775354))"));
    }

    @Test
    public void parseLocation() throws IOException, StudyImporterException {
        final String locationLines = "REF_ID,PRED_ID,LOC_ID,LME_CODE,COUNTRY,STATE,GULF_OCTANT,LOCALE_NAME,LOCALE_TYPE,BAY_HIERARC_LEVEL,SAMP_DEP_REALM,LOC_CENTR_LAT,LOC_CENTR_LONG,LOC_POLY_COORDS,LOC_POLY_SOURCE,LOC_POLY_CONF,MAJOR_LOC_NAME,MAJOR_LOC_TYPE,MIN_DEP_LOC,MAX_DEP_LOC,MN_DEP_LOC,MIN_DEP_SAMP,MAX_DEP_SAMP,MN_DEP_SAMP,TIME_ZONE,START_YEAR,START_MON,START_DAY,START_TIME,START_SEAS,END_YEAR,END_MON,END_DAY,END_TIME,END_SEAS,HAB_SYSTEM,HAB_SUBSYSTEM,TIDAL_ZONE,REALM,PROVINCE,ECOREGION,SYS_CONF,LOC_NOTES,NUM_SAMPS,STA_DATA\n" +
                "16r,Cchr.1,16r.1,GOM,US,LA,NNW,Louisiana inner continental shelf,Continental shelf,NA,Demersal,29.346953,-92.980614,\"((-92.6729107838999,29.3941413332999),(-92.5604838626999,29.2066775354),(-92.7326173694,29.1150784684999),(-92.9638307704999,29.1171045174),(-93.3169089704999,29.3616452463),(-93.4007435505999,29.5222620776999),(-93.3169089704999,29.6243402981),(-93.1045280342,29.6340566488),(-92.6729107838999,29.3941413332999))\",Inferred from station locations,high,Louisiana continental shelf,Gulf/Ocean waters,9.094,18.188,13.641,9.094,18.188,13.641,CDST,1970,7,NA,0:00,summer,1973,2,NA,0:00,winter,Marine,Nearshore,Subtidal,TNA,WTNA,43,NA,NA,NA,yes\n" +
                "16r,Cchr.1,16r.2,GOM,US,LA,NNW,Louisiana mid continental shelf,Continental shelf,NA,Demersal,29.032598,-92.287009,\"((-92.1815365767999,29.1068886358999),(-92.200079333,29.0069336331),(-92.3030548952999,28.9075567952),(-92.4770626883999,28.8892759983999),(-92.5999700862,28.9468493538999),(-92.6034249349,29.0088070450999),(-92.5255192441999,29.0774757406),(-92.3918542331,29.148292841),(-92.2231376575,29.1384141105999),(-92.1815365767999,29.1068886358999))\",Inferred from station locations,high,Louisiana continental shelf,Gulf/Ocean waters,18.188,54.564,36.376,18.188,54.564,36.376,CDST,1970,7,NA,0:00,summer,1973,2,NA,0:00,winter,Marine,Offshore,Subtidal,TNA,WTNA,43,NA,NA,NA,yes\n";

        final LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(IOUtils.toInputStream(locationLines)));
        parser.getLine();
        final Location location = StudyImporterForGoMexSI.parseLocation("someresropuce.csv", parser);
        assertThat(location.getLatitude(), is(29.346953d));
        assertThat(location.getLongitude(), is(-92.980614d));
        assertThat(location.getAltitude(), is(-13.641d));
        assertThat(location.getFootprintWKT(), is("POLYGON((-92.6729107838999 29.3941413332999),(-92.5604838626999 29.2066775354),(-92.7326173694 29.1150784684999),(-92.9638307704999 29.1171045174),(-93.3169089704999 29.3616452463),(-93.4007435505999 29.5222620776999),(-93.3169089704999 29.6243402981),(-93.1045280342 29.6340566488),(-92.6729107838999 29.3941413332999))"));
    }

}