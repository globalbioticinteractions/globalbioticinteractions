package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.SpecimenConstant;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForGoMexSI2Test {

    public static final String WKT_FOOTPRINT = "POLYGON((-92.6729107838999 29.3941413332999,-92.5604838626999 29.2066775354,-92.7326173694 29.1150784684999,-92.9638307704999 29.1171045174,-93.3169089704999 29.3616452463,-93.4007435505999 29.5222620776999,-93.3169089704999 29.6243402981,-93.1045280342 29.6340566488,-92.6729107838999 29.3941413332999))";

    @Test
    public void importSingleLine() throws IOException, StudyImporterException {
        final Map<String, String> parsedProperties = new HashMap<String, String>();
        String predOneLine = "DATA_ID,PRED_ID,DATA_AGGR_CODE,PRED_SOURCE_NAME,PRED_DATABASE_NAME,PRED_TOT_STOM_EXAM,TOT_W_FD,PCT_W_FD,TOT_WO_FD,PCT_WO_FD,PRED_DEN,PRED_SEX,PRED_SEX_RATIO,PRED_LIFE_HIST_STAGE,PRED_LEN_TYPE_1,PRED_MIN_LEN_1,PRED_MAX_LEN_1,PRED_MN_LEN_1,PRED_LEN_TYPE_2,PRED_MIN_LEN_2,PRED_MAX_LEN_2,PRED_MN_LEN_2,PRED_LEN_TYPE_3,PRED_MIN_LEN_3,PRED_MAX_LEN_3,PRED_MN_LEN_3,PRED_MIN_AGE,PRED_MAX_AGE,PRED_MN_AGE,PRED_MIN_WT,PRED_MAX_WT,PRED_MN_WT,PRED_TOT_WT,BIOMASS_DEN,STOM_PCT_FULL,STOM_COND_INDEX,TOT_FD_BIOMASS,TOT_FD_VOL,PREDATOR_NOTES,POP_TOT_PRED,POP_PRED_DEN,POP_SEX_RATIO,POP_LIFE_HIST_STAGE,POP_LEN_TYPE,POP_MIN_LEN,POP_MAX_LEN,POP_MN_LEN,POP_MIN_AGE,POP_MAX_AGE,POP_MN_AGE,POP_MIN_WT,POP_MAX_WT,POP_MN_WT,POP_TOT_WT,POP_BIOMASS_DEN,ENTRY_DATE,ENTRY_PERSON,EDITED_DATE,DATA_EDITOR,MODIFY_DATE,DATA_MODIFIER\n" +
                "ACT_16R,Cchr.1,complete,Chloroscombrus chrysurus,Chloroscombrus chrysurus,66,61,0.9242,5,0.0758,NA,NA,NA,adult,SL,101,150,125.5,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,8.3,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,Jim Simons,NA,Jim Simons,27/06/2016,Theresa Mitchell\n";
        StudyImporterForGoMexSI2.parseSpecimen("test.txt", "PRED_", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                parsedProperties.putAll(properties);
            }
        }, new LabeledCSVParser(new CSVParser(new StringReader(predOneLine))));

        assertThat(parsedProperties.get("name"), is("Chloroscombrus chrysurus"));
        assertThat(parsedProperties.get("GOMEXSI:PRED_DATABASE_NAME"), is("Chloroscombrus chrysurus"));
    }

    @Test
    public void importSinglePrey() throws IOException, StudyImporterException {
        final Map<String, String> parsedProperties = new HashMap<String, String>();
        String predOneLine = "DATA_ID,PRED_ID,PREY_SOURCE_NAME,PREY_DATABASE_NAME,PHYSIOLOG_STATE,SED_ORIGIN,PREY_PARTS,PREY_LIFE_HIST_STAGE,PREY_COND_INDEX,PREY_SEX,PREY_SEX_RATIO,PREY_LEN_TYPE,PREY_MIN_LEN,PREY_MAX_LEN,PREY_MN_LEN,PREY_MIN_WIDTH,PREY_MAX_WIDTH,PREY_MN_WIDTH,BIOMASS,BIOMASS_QUALIFIER,PCT_BIOMASS,PCT_BIOMASS_QUALIFIER,N_CONS,N_CONS_QUALIFIER,PCT_N_CONS,PCT_N_CONS_QUALIFIER,VOL_CONS,VOL_CONS_QUALIFIER,PCT_VOL_CONS,PCT_VOL_CONS_QUALIFIER,FREQ_OCC,FREQ_OCC_QUALIFIER,PCT_FREQ_OCC,PCT_FREQ_OCC_QUALIFIER,IRI,PCT_IRI,IRIa,IIR,E,PREY_NOTES,ENTRY_DATE,ENTRY_PERSON,EDITED_DATE,DATA_EDITOR,MODIFY_DATE,DATA_MODIFIER\n" +
                "ACT_16R,Cchr.1,Crustacea,Crustacea,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,1.245,NA,0.15,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,Jim Simons,NA,Jim Simons,27/06/2016,Theresa Mitchell\n";
        StudyImporterForGoMexSI2.parseSpecimen("test.txt", "PREY_", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                parsedProperties.putAll(properties);
            }
        }, new LabeledCSVParser(new CSVParser(new StringReader(predOneLine))));

        assertThat(parsedProperties.get("name"), is("Crustacea"));
        assertThat(parsedProperties.get("GOMEXSI:PREY_SOURCE_NAME"), is("Crustacea"));
    }

    @Test
    public void importSinglePreyMissingDatabaseName() throws IOException, StudyImporterException {
        final Map<String, String> parsedProperties = new HashMap<String, String>();
        String predOneLine = "DATA_ID,PRED_ID,PREY_SOURCE_NAME,PREY_DATABASE_NAME,PHYSIOLOG_STATE,SED_ORIGIN,PREY_PARTS,PREY_LIFE_HIST_STAGE,PREY_COND_INDEX,PREY_SEX,PREY_SEX_RATIO,PREY_LEN_TYPE,PREY_MIN_LEN,PREY_MAX_LEN,PREY_MN_LEN,PREY_MIN_WIDTH,PREY_MAX_WIDTH,PREY_MN_WIDTH,BIOMASS,BIOMASS_QUALIFIER,PCT_BIOMASS,PCT_BIOMASS_QUALIFIER,N_CONS,N_CONS_QUALIFIER,PCT_N_CONS,PCT_N_CONS_QUALIFIER,VOL_CONS,VOL_CONS_QUALIFIER,PCT_VOL_CONS,PCT_VOL_CONS_QUALIFIER,FREQ_OCC,FREQ_OCC_QUALIFIER,PCT_FREQ_OCC,PCT_FREQ_OCC_QUALIFIER,IRI,PCT_IRI,IRIa,IIR,E,PREY_NOTES,ENTRY_DATE,ENTRY_PERSON,EDITED_DATE,DATA_EDITOR,MODIFY_DATE,DATA_MODIFIER\n" +
                "ACT_16R,Cchr.1,Crustacea,,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,1.245,NA,0.15,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,Jim Simons,NA,Jim Simons,27/06/2016,Theresa Mitchell\n";
        StudyImporterForGoMexSI2.parseSpecimen("test.txt", "PREY_", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                parsedProperties.putAll(properties);
            }
        }, new LabeledCSVParser(new CSVParser(new StringReader(predOneLine))));

        assertThat(parsedProperties.get("name"), is("Crustacea"));
        assertThat(parsedProperties.get("GOMEXSI:PREY_SOURCE_NAME"), is("Crustacea"));
        assertThat(parsedProperties.get("GOMEXSI:PREY_DATABASE_NAME"), is(""));
    }

    @Test
    public void importSinglePreyWithPCTValues() throws IOException, StudyImporterException {
        final Map<String, String> parsedProperties = new HashMap<String, String>();
        String predOneLine = "DATA_ID,PRED_ID,PREY_SOURCE_NAME,PREY_DATABASE_NAME,PHYSIOLOG_STATE,SED_ORIGIN,PREY_PARTS,PREY_LIFE_HIST_STAGE,PREY_COND_INDEX,PREY_SEX,PREY_SEX_RATIO,PREY_LEN_TYPE,PREY_MIN_LEN,PREY_MAX_LEN,PREY_MN_LEN,PREY_MIN_WIDTH,PREY_MAX_WIDTH,PREY_MN_WIDTH,BIOMASS,BIOMASS_QUALIFIER,PCT_BIOMASS,PCT_BIOMASS_QUALIFIER,N_CONS,N_CONS_QUALIFIER,PCT_N_CONS,PCT_N_CONS_QUALIFIER,VOL_CONS,VOL_CONS_QUALIFIER,PCT_VOL_CONS,PCT_VOL_CONS_QUALIFIER,FREQ_OCC,FREQ_OCC_QUALIFIER,PCT_FREQ_OCC,PCT_FREQ_OCC_QUALIFIER,IRI,PCT_IRI,IRIa,IIR,E,PREY_NOTES,ENTRY_DATE,ENTRY_PERSON,EDITED_DATE,DATA_EDITOR,MODIFY_DATE,DATA_MODIFIER\n" +
                "ACT_16R,Cchr.1,Crustacea,Crustacea,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,0.6,NA,0.5,NA,0.4,NA,0.3,NA,0.2,NA,0.1,NA,NA,NA,NA,NA,NA,NA,NA,Jim Simons,NA,Jim Simons,27/06/2016,Theresa Mitchell\n";
        StudyImporterForGoMexSI2.parseSpecimen("test.txt", "PREY_", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                parsedProperties.putAll(properties);
            }
        }, new LabeledCSVParser(new CSVParser(new StringReader(predOneLine))));

        assertThat(parsedProperties.get("name"), is("Crustacea"));
        assertThat(parsedProperties.get("GOMEXSI:PREY_SOURCE_NAME"), is("Crustacea"));
        assertThat(parsedProperties.get("GOMEXSI:N_CONS"), is("0.6"));
        assertThat(parsedProperties.get("GOMEXSI:PCT_N_CONS"), is("0.5"));
        assertThat(parsedProperties.get("GOMEXSI:VOL_CONS"), is("0.4"));
        assertThat(parsedProperties.get("GOMEXSI:PCT_VOL_CONS"), is("0.3"));
        assertThat(parsedProperties.get("GOMEXSI:FREQ_OCC"), is("0.2"));
        assertThat(parsedProperties.get("GOMEXSI:PCT_FREQ_OCC"), is("0.1"));

        assertThat(parsedProperties.get(SpecimenConstant.TOTAL_COUNT), is("0.6"));
        assertThat(parsedProperties.get(SpecimenConstant.TOTAL_COUNT_PERCENT), is("0.5"));
        assertThat(parsedProperties.get(SpecimenConstant.TOTAL_VOLUME_IN_ML), is("0.4"));
        assertThat(parsedProperties.get(SpecimenConstant.TOTAL_VOLUME_PERCENT), is("0.3"));
        assertThat(parsedProperties.get(SpecimenConstant.FREQUENCY_OF_OCCURRENCE), is("0.2"));
        assertThat(parsedProperties.get(SpecimenConstant.FREQUENCY_OF_OCCURRENCE_PERCENT), is("0.1"));
    }

    @Test
    public void importReferences() throws IOException, StudyImporterException {
        String someReferences = "REF_ID,DATA_ID,DATA_TYPE,GAME_ID,DATA_SOURCE_TYPE,THESIS_TYPE,REF_TAG,AUTH_L_NAME,AUTH_SUFFIX,AUTH_F_NAME,AUTH_M_INIT,YEAR_PUB,TITLE_REF,EDITOR_L_NAME,EDITOR_F_NAME,EDITOR_M_INIT,SOURCE_NAME,VOL,NUM,START_PAGE,END_PAGE,DOI,PUB_NAME,PUB_CITY,PUB_STATE,PUB_COUNTRY,UNIV_NAME,UNIV_CITY,UNIV_STATE,UNIV_COUNTRY,REF_NOTES,LANGUAGE,REF_POLY_SHAPEFILE_ID,REF_CENT_SHAPEFILE_ID,REF_STAT_SHAPEFILE_ID,ENTRY_DATE,ENTRY_PERSON,EDITED_DATE,DATA_EDITOR,MODIFY_DATE,DATA_MODIFIER\n" +
                "ACT_5A,ACT_5A,pred/prey,2510,PL,NA,\"Adams and Kendall, 1891\",Adams,NA,A,C,1891,Report upon an investigation of fishing grounds off the west coast of Florida,NA,NA,NA,Bulletin of the United States Fish Commission,NA,NA,289,312,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,English,NA,NA,NA,11/10/2012,Amanda Yzaguirre,NA,Amanda Yzaguirre,15/06/2016,Theresa Mitchell\n" +
                "ACT_5A,ACT_5A,pred/prey,2510,PL,NA,\"Adams and Kendall, 1891\",Kendall,NA,W,C,1891,Report upon an investigation of fishing grounds off the west coast of Florida,NA,NA,NA,Bulletin of the United States Fish Commission,NA,NA,289,312,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,English,NA,NA,NA,12/10/2012,Amanda Yzaguirre,NA,Amanda Yzaguirre,15/06/2016,Theresa Mitchell\n" +
                "ACT_14A,ACT_14A,pred/prey,2519,T,BS,Alarcon Sosa 2007,Alarcon Sosa,NA,Adlemy,C,2007,\"Aspectos trĂłficos de la ictiofauna de la laguna de Sontecomapan, Ver. Durante la temporada de secas del 2005. \",NA,NA,NA,NA,NA,NA,1,75,NA,NA,NA,NA,NA,\"Universidad Nacional AutĂłnoma de Mexico, FES-Iztacala\",Mexico City,Distrito Federal,Mexico,NA,Spanish,NA,NA,NA,NA,Jim Jim Simons,NA,Jim Jim Simons,15/06/2016,Theresa Mitchell\n" +
                "ACT_2B,ACT_2B,pred/prey,2538,T,MS,\"Bailey, 1995\",Bailey,IV,H,Killebrew,1995,Potential Interactive Effects of Habitat Complexity and Sub-adults on Young of the Year Red Snapper (Lutjanus campechanus) Behavior,NA,NA,NA,NA,NA,NA,1,51,NA,NA,NA,NA,NA,University of South Alabama,Mobile,AL,US,NA,English,NA,NA,NA,22/02/2014,Theresa Mitchell,22/02/2014,Theresa Mitchell,15/06/2016,Theresa Mitchell\n" +
                "ACT_14B,ACT_14B,pred/prey,2569,PL,NA,\"Baughman, 1943\",Baughman,NA,J,L,1943,The Lutianid Fishes of Texas,NA,NA,NA,Copeia,1943,4,212,215,NA,American Society of Ichthyologists and Herpetologists,Lawrence,KS,US,NA,NA,NA,NA,NA,English,NA,NA,NA,06/11/2013,Theresa Mitchell,29/01/2014,Theresa Mitchell,15/06/2016,Theresa Mitchell\n" +
                "ACT_16B,ACT_16B,pred/prey,2549,PL,NA,Baughman and Springer 1950,Baughman,NA,J,L,1950,\"Biological and Economic Notes on the Sharks of the Gulf of Mexico, With Especial Reference to Those of Texas, and With a Key for their Identification\",NA,NA,NA,The American Midland Naturalist,44,1,96,152,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,English,NA,NA,NA,19/09/2012,Theresa Mitchell,25/01/2013,Theresa Mitchell,15/06/2016,Theresa Mitchell\n" +
                "ACT_16B,ACT_16B,pred/prey,2549,PL,NA,Baughman and Springer 1950,Springer,NA,Stewart,NA,1950,\"Biological and Economic Notes on the Sharks of the Gulf of Mexico, With Especial Reference to Those of Texas, and With a Key for their Identification\",NA,NA,NA,The American Midland Naturalist,44,1,96,152,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,English,NA,NA,NA,19/09/2012,Theresa Mitchell,25/01/2013,Theresa Mitchell,15/06/2016,Theresa Mitchell\n" +
                "ACT_17B,ACT_17B,pred/prey,2550,GR,NA,Beaumariage 1973,Beaumariage,NA,Dale,S,1973,\"Age, growth, and reproduction of king mackerel, Scomberomorus cavalla, in Florida.  \",NA,NA,NA,Florida Marine Research Publications,NA,1,1,45,,\"Marine Research Laboratory, Florida Department of Natural Resources\",St. Petersburg,FL,US,NA,NA,NA,NA,NA,English,NA,NA,NA,NA,Jim Jim Simons,NA,Jim Jim Simons,15/06/2016,Theresa Mitchell\n" +
                "ACT_18B,ACT_18B,pred/prey,3178,GR,NA,\"Beaumariage and Bullock, 1976\",Beaumariage,NA,D,S,1976,Biological Research on Snappers and Groupers as Related to Fishery Management Requirements,\"Bullis, Jr.\",Harvey,R,Proceedings: Colloquium on Snapper-Grouper Fishery Resources of the Western Central Atlantic Ocean,1976,17,86,94,NA,Florida Sea Grant College Program,St. Petersberg,FL,US,NA,NA,NA,NA,NA,English,NA,NA,NA,06/11/2013,Theresa Mitchell,29/01/2014,Theresa Mitchell,15/06/2016,Theresa Mitchell\n" +
                "ACT_18B,ACT_18B,pred/prey,3178,GR,NA,\"Beaumariage and Bullock, 1976\",Beaumariage,NA,D,S,1976,Biological Research on Snappers and Groupers as Related to Fishery Management Requirements,Jones,Albert,C,Proceedings: Colloquium on Snapper-Grouper Fishery Resources of the Western Central Atlantic Ocean,1976,17,86,94,NA,Florida Sea Grant College Program,St. Petersberg,FL,US,NA,NA,NA,NA,NA,English,NA,NA,NA,06/11/2013,Theresa Mitchell,29/01/2014,Theresa Mitchell,15/06/2016,Theresa Mitchell\n";

        Map<String, String> contributorMap = StudyImporterForGoMexSI2.collectContributors("bla", new LabeledCSVParser(new CSVParser(new StringReader(someReferences))));

        assertThat(contributorMap.get("ACT_5A"), is("A Adams, W Kendall"));
        assertThat(contributorMap.get("ACT_14A"), is("Adlemy Alarcon Sosa"));
        assertThat(contributorMap.get("ACT_2B"), is("H Bailey"));
        assertThat(contributorMap.get("ACT_14B"), is("J Baughman"));
        assertThat(contributorMap.get("ACT_16B"), is("J Baughman, Stewart Springer"));
        assertThat(contributorMap.get("ACT_17B"), is("Dale Beaumariage"));
        assertThat(contributorMap.get("ACT_18B"), is("D Beaumariage, D Beaumariage"));
    }

    @Test
    public void polyCoordToWKT() {
        assertThat(StudyImporterForGoMexSI2.polyCoordsToWKT("((-92.6729107838999,29.3941413332999),(-92.5604838626999,29.2066775354))")
                , is("POLYGON((-92.6729107838999 29.3941413332999,-92.5604838626999 29.2066775354))"));
    }

    @Test
    public void parseLocation() throws IOException, StudyImporterException {
        final String locationLines = "DATA_ID,PRED_ID,LOC_ID,LME_CODE,COUNTRY,STATE,GULF_OCTANT,LOCALE_NAME,LOCALE_TYPE,BAY_HIERARC_LEVEL,SAMP_DEP_REALM,LOC_CENTR_LAT,LOC_CENTR_LONG,LOC_POLY_COORDS,PRED_POLY_SHAPEFILE_ID,PRED_CENT_SHAPEFILE_ID,PRED_STAT_SHAPEFILE_ID,LOC_POLY_SOURCE,LOC_POLY_CONF,MAJOR_LOC_NAME,MAJOR_LOC_TYPE,MIN_DEP_LOC,MAX_DEP_LOC,MN_DEP_LOC,MIN_DEP_SAMP,MAX_DEP_SAMP,MN_DEP_SAMP,TIME_ZONE,START_YEAR,START_MON,START_DAY,START_TIME,START_SEAS,END_YEAR,END_MON,END_DAY,END_TIME,END_SEAS,REALM,PROVINCE,ECOREGION,HAB_SYSTEM,HAB_SUBSYSTEM,TIDAL_ZONE,SYS_CONF,LOC_NOTES,NUM_SAMPS,ENTRY_DATE,ENTRY_PERSON,EDITED_DATE,DATA_EDITOR,MODIFY_DATE,DATA_MODIFIER\n" +
                "ACT_16R,Cchr.1,ACT_16r.1,GOM,US,LA,NNW,Louisiana inner continental shelf,Continental shelf,NA,Demersal,29.346953,-92.980614,\"((-92.6729107838999,29.3941413332999),(-92.5604838626999,29.2066775354),(-92.7326173694,29.1150784684999),(-92.9638307704999,29.1171045174),(-93.3169089704999,29.3616452463),(-93.4007435505999,29.5222620776999),(-93.3169089704999,29.6243402981),(-93.1045280342,29.6340566488),(-92.6729107838999,29.3941413332999))\",,,,Inferred from station locations,high,Louisiana continental shelf,Gulf/Ocean waters,9.094,18.188,13.641,9.094,18.188,13.641,CDST,1970,7,NA,0:00,summer,1973,2,NA,0:00,winter,TNA,WTNA,43,Marine,Nearshore,Subtidal,NA,NA,NA,NA,Jim Simons,NA,Jim Simons,15/06/2016,Theresa Mitchell\n" +
                "ACT_16R,Cchr.1,ACT_16r.2,GOM,US,LA,NNW,Louisiana mid continental shelf,Continental shelf,NA,Demersal,29.032598,-92.287009,\"((-92.1815365767999,29.1068886358999),(-92.200079333,29.0069336331),(-92.3030548952999,28.9075567952),(-92.4770626883999,28.8892759983999),(-92.5999700862,28.9468493538999),(-92.6034249349,29.0088070450999),(-92.5255192441999,29.0774757406),(-92.3918542331,29.148292841),(-92.2231376575,29.1384141105999),(-92.1815365767999,29.1068886358999))\",,,,Inferred from station locations,high,Louisiana continental shelf,Gulf/Ocean waters,18.188,54.564,36.376,18.188,54.564,36.376,CDST,1970,7,NA,0:00,summer,1973,2,NA,0:00,winter,TNA,WTNA,43,Marine,Offshore,Subtidal,NA,NA,NA,NA,Jim Simons,NA,Jim Simons,15/06/2016,Theresa Mitchell\n" +
                "ACT_16R,Cchr.1,ACT_16r.1,GOM,US,LA,NNW,Louisiana inner continental shelf,Continental shelf,NA,Demersal,29.346953,-92.980614,\"((-92.6729107838999,29.3941413332999),(-92.5604838626999,29.2066775354),(-92.7326173694,29.1150784684999),(-92.9638307704999,29.1171045174),(-93.3169089704999,29.3616452463),(-93.4007435505999,29.5222620776999),(-93.3169089704999,29.6243402981),(-93.1045280342,29.6340566488),(-92.6729107838999,29.3941413332999))\",,,,Inferred from station locations,high,Louisiana continental shelf,Gulf/Ocean waters,9.094,18.188,13.641,9.094,18.188,13.641,CDST,1970,7,NA,0:00,summer,1973,2,NA,0:00,winter,TNA,WTNA,43,Marine,Nearshore,Subtidal,NA,NA,NA,NA,Jim Simons,NA,Jim Simons,15/06/2016,Theresa Mitchell\n" +
                "ACT_16R,Cchr.1,ACT_16r.2,GOM,US,LA,NNW,Louisiana mid continental shelf,Continental shelf,NA,Demersal,29.032598,-92.287009,\"((-92.1815365767999,29.1068886358999),(-92.200079333,29.0069336331),(-92.3030548952999,28.9075567952),(-92.4770626883999,28.8892759983999),(-92.5999700862,28.9468493538999),(-92.6034249349,29.0088070450999),(-92.5255192441999,29.0774757406),(-92.3918542331,29.148292841),(-92.2231376575,29.1384141105999),(-92.1815365767999,29.1068886358999))\",,,,Inferred from station locations,high,Louisiana continental shelf,Gulf/Ocean waters,18.188,54.564,36.376,18.188,54.564,36.376,CDST,1970,7,NA,0:00,summer,1973,2,NA,0:00,winter,TNA,WTNA,43,Marine,Offshore,Subtidal,NA,NA,NA,NA,Jim Simons,NA,Jim Simons,15/06/2016,Theresa Mitchell\n";

        final LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(IOUtils.toInputStream(locationLines)));
        parser.getLine();
        final Location location = StudyImporterForGoMexSI2.parseLocation("someMickeyMouseresource.csv", parser);
        assertThat(location.getLatitude(), is(29.346953d));
        assertThat(location.getLongitude(), is(-92.980614d));
        assertThat(location.getAltitude(), is(-13.641d));
        assertThat(location.getLocality(), is("Louisiana inner continental shelf"));
        assertThat(location.getFootprintWKT(), is(WKT_FOOTPRINT));
    }

}