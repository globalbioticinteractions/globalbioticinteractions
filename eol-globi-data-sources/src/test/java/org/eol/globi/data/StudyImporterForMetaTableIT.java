package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class StudyImporterForMetaTableIT {

    @Test
    public void importAll() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = new InteractionListener() {

            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                links.add(properties);
            }
        };
        final StudyImporterForMetaTable.TableParserFactory tableFactory = new StudyImporterForMetaTable.TableParserFactory() {

            public CSVParse createParser(JsonNode config) throws IOException {
                String firstFewLines = "intertype,obstype,effunit,effort,obsunit,obsquant,germnotes,\"REPLACE(Interaction.notes, ',', ';')\",AnimalNumber,AnimalClass,AnimalOrder,AnimalFamily,AnimalGenus,AnimalSpecies,AnimalSubSpecies,AnimalType,AnimalCommonName,PlantNumber,PlantFamily,PlantGenus,PlantSpecies,PlantSubSpecies,country,region,ProvinceDistrictCity,ProtectedArea,HabitatWhite,HabitatAuthor,author,title,year,journal,volume,number,pages,USER,DEF_timestamp,,,\n" +
                        "seed disperser,direct observation,months,4,dung density,,,Article focused on elephant density per habitat type based on seed/plant types identified in dung at the various research locations. All identified plant types are being assumed to be dispersed by the elephants,1441,Mammalia,Proboscidea,Elephantidae,Loxodonta,africana,,NULL,African Bush Elephant,4035,Poaceae,Cynodon,dactylon,NULL,Mozambique,NULL,NULL,yes,forest transitions and mosaics,mangroves dune grass plains forest woodland riverine,\"De Boer, W.F. and Ntumi, C.P. and Correia, A.U. and Mafuca, J.M.\",Diet and distribution of elephant in the Maputo Elephant Reserve; Mozambique,2000,African Journal of Ecology,38,3,188-201,Mary,0000-00-00 00:00:00,,,\n" +
                        "seed disperser,direct observation,months,4,dung density,,,Article focused on elephant density per habitat type based on seed/plant types identified in dung at the various research locations. All identified plant types are being assumed to be dispersed by the elephants,1441,Mammalia,Proboscidea,Elephantidae,Loxodonta,africana,,NULL,African Bush Elephant,3639,Poaceae,Aristida,canescens,NULL,Mozambique,NULL,NULL,yes,forest transitions and mosaics,mangroves dune grass plains forest woodland riverine,\"De Boer, W.F. and Ntumi, C.P. and Correia, A.U. and Mafuca, J.M.\",Diet and distribution of elephant in the Maputo Elephant Reserve; Mozambique,2000,African Journal of Ecology,38,3,188-201,Mary,0000-00-00 00:00:00,,,\n" +
                        "seed disperser,direct observation,months,4,dung density,,,Article focused on elephant density per habitat type based on seed/plant types identified in dung at the various research locations. All identified plant types are being assumed to be dispersed by the elephants,1441,Mammalia,Proboscidea,Elephantidae,Loxodonta,africana,,NULL,African Bush Elephant,3574,Poaceae,Andropogon,eucomus,NULL,Mozambique,NULL,NULL,yes,forest transitions and mosaics,mangroves dune grass plains forest woodland riverine,\"De Boer, W.F. and Ntumi, C.P. and Correia, A.U. and Mafuca, J.M.\",Diet and distribution of elephant in the Maputo Elephant Reserve; Mozambique,2000,African Journal of Ecology,38,3,188-201,Mary,0000-00-00 00:00:00,,,\n" +
                        "seed disperser,direct observation,months,4,dung density,,,Article focused on elephant density per habitat type based on seed/plant types identified in dung at the various research locations. All identified plant types are being assumed to be dispersed by the elephants,1441,Mammalia,Proboscidea,Elephantidae,Loxodonta,africana,,NULL,African Bush Elephant,5125,Phyllanthaceae,Phyllanthus,reticulatus,NULL,Mozambique,NULL,NULL,yes,forest transitions and mosaics,mangroves dune grass plains forest woodland riverine,\"De Boer, W.F. and Ntumi, C.P. and Correia, A.U. and Mafuca, J.M.\",Diet and distribution of elephant in the Maputo Elephant Reserve; Mozambique,2000,African Journal of Ecology,38,3,188-201,Mary,0000-00-00 00:00:00,,,\n" +
                        "seed disperser,direct observation,months,4,dung density,,,Article focused on elephant density per habitat type based on seed/plant types identified in dung at the various research locations. All identified plant types are being assumed to be dispersed by the elephants,1441,Mammalia,Proboscidea,Elephantidae,Loxodonta,africana,,NULL,African Bush Elephant,399,Myrtaceae,Syzygium,cordatum,,Mozambique,NULL,NULL,yes,forest transitions and mosaics,mangroves dune grass plains forest woodland riverine,\"De Boer, W.F. and Ntumi, C.P. and Correia, A.U. and Mafuca, J.M.\",Diet and distribution of elephant in the Maputo Elephant Reserve; Mozambique,2000,African Journal of Ecology,38,3,188-201,Mary,0000-00-00 00:00:00,,,\n" +
                        "seed disperser,direct observation,months,4,dung density,,,Article focused on elephant density per habitat type based on seed/plant types identified in dung at the various research locations. All identified plant types are being assumed to be dispersed by the elephants,1441,Mammalia,Proboscidea,Elephantidae,Loxodonta,africana,,NULL,African Bush Elephant,374,Moraceae,Ficus,sycomorus,,Mozambique,NULL,NULL,yes,forest transitions and mosaics,mangroves dune grass plains forest woodland riverine,\"De Boer, W.F. and Ntumi, C.P. and Correia, A.U. and Mafuca, J.M.\",Diet and distribution of elephant in the Maputo Elephant Reserve; Mozambique,2000,African Journal of Ecology,38,3,188-201,Mary,0000-00-00 00:00:00,,,\n" +
                        "seed disperser,direct observation,months,4,dung density,,,Article focused on elephant density per habitat type based on seed/plant types identified in dung at the various research locations. All identified plant types are being assumed to be dispersed by the elephants,1441,Mammalia,Proboscidea,Elephantidae,Loxodonta,africana,,NULL,African Bush Elephant,4398,Moraceae,Ficus,sp,NULL,Mozambique,NULL,NULL,yes,forest transitions and mosaics,mangroves dune grass plains forest woodland riverine,\"De Boer, W.F. and Ntumi, C.P. and Correia, A.U. and Mafuca, J.M.\",Diet and distribution of elephant in the Maputo Elephant Reserve; Mozambique,2000,African Journal of Ecology,38,3,188-201,Mary,0000-00-00 00:00:00,,,\n" +
                        "seed disperser,direct observation,years,4,NULL,NULL,NULL,NULL,3051,Animal,Animal,Animal,Animal,animal,NULL,general animal,NULL,4176,Caesalpinioideae,Distemonanthus,benthamianus,NULL,Cameroon,NULL,NULL,yes,NULL,semideciduous tropical rain forest,\"Hardesty, B.D. and Parker, V.T.\",Community seed rain patterns and a comparison to adult community structure in a West African tropical forest,2003,Plant Ecology,164,1,49-64,Mary,8/15/12 9:35,,,\n" +
                        "ingestion,direct observation,years,2,NULL,NULL,NULL,during both summer and winter season,1462,Mammalia,Artiodactyla,Bovidae,Madoqua,kirkii,,NULL,Kirk's Dikdik,6897,Moraceae,Ficus,petersii,NULL,Namibia,South West Africa,NULL,yes,NULL,riverine thicket,\"Tinley, K.\",Dikdik; Madoqua kirkii; in south-west Africa: notes on distribution; ecology; and behaviour,1969,Madoqua,1,NULL,Jul-33,Anna,2/24/14 18:40,,,\n";

                return new LabeledCSVParser(new CSVParser(IOUtils.toInputStream(firstFewLines)));
            }
        };


        final String resource = "https://raw.githubusercontent.com/globalbioticinteractions/AfricaTreeDatabase/master/globi.json";

        importAll(interactionListener, tableFactory, resource);
        assertThat(links.size(), is(9));
    }

    @Test
    public void importREEMWithStaticCSV() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = new InteractionListener() {

            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                links.add(properties);
            }
        };

        final StudyImporterForMetaTable.TableParserFactory tableFactory = new StudyImporterForMetaTable.TableParserFactory() {

            public CSVParse createParser(JsonNode config) throws IOException {
                String firstFewLines = "Hauljoin,\" Pred_nodc\",\" Pred_specn\",\" Prey_nodc\",\" Pred_len\",\" Year\",\" Month\",\" day\",\" region\",\" Pred_name\",\" Prey_Name\",\" Vessel\",\" Cruise\",\" Haul\",\" Rlat\",\" Rlong\",\" Gear_depth\",\" Bottom_depth\",\" Start_hour\",\" Surface_temp\",\" Gear_temp\",\" INPFC_Area\",\" Stationid\",\" Start_date\",\" Prey_sz1\",\" Prey_sex\"\n" +
                        "11012118.0,8791030401.0,5.0,9999999998.0,53.0,1994.0,7.0,11.0,AI,\"Pacific cod Gadus macrocephalus\",\"Rocks \",95.0,199401.0,148.0,51.43,178.81999999999999,222.0,228.0,11.0,0.63,0.41999999999999998,542.0,118-11,\"1994-07-11 00:00:00\",3.0,\n" +
                        "11012118.0,8791030401.0,8.0,9999999998.0,53.0,1994.0,7.0,11.0,AI,\"Pacific cod Gadus macrocephalus\",\"Rocks \",95.0,199401.0,148.0,51.43,178.81999999999999,222.0,228.0,11.0,0.63,0.41999999999999998,542.0,118-11,\"1994-07-11 00:00:00\",3.0,\n" +
                        "11012118.0,8791030401.0,9.0,9999999998.0,58.0,1994.0,7.0,11.0,AI,\"Pacific cod Gadus macrocephalus\",\"Rocks \",95.0,199401.0,148.0,51.43,178.81999999999999,222.0,228.0,11.0,0.63,0.41999999999999998,542.0,118-11,\"1994-07-11 00:00:00\",13.0,\n" +
                        "11012118.0,8791030401.0,9.0,9999999998.0,58.0,1994.0,7.0,11.0,AI,\"Pacific cod Gadus macrocephalus\",\"Rocks \",95.0,199401.0,148.0,51.43,178.81999999999999,222.0,228.0,11.0,0.63,0.41999999999999998,542.0,118-11,\"1994-07-11 00:00:00\",3.0,\n";

                return new LabeledCSVParser(new CSVParser(IOUtils.toInputStream(firstFewLines)));
            }
        };

        final String resource = "https://raw.githubusercontent.com/globalbioticinteractions/noaa-reem/master/globi.json";
        importAll(interactionListener, tableFactory, resource);

        assertThat(links.size(), is(12));

        final Map<String, String> firstLine = links.get(0);
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_ID), is(nullValue()));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Rocks"));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_ID), is("NODC:8791030401"));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Pacific cod Gadus macrocephalus"));
        assertThat(firstLine.get(StudyImporterForMetaTable.EVENT_DATE), startsWith("19940711"));
        assertThat(firstLine.get(StudyImporterForMetaTable.LATITUDE), is("51.43"));
        assertThat(firstLine.get(StudyImporterForMetaTable.LONGITUDE), is("178.81999999999999"));
    }

    static public void importAll(InteractionListener interactionListener, StudyImporterForMetaTable.TableParserFactory tableFactory, String resource) throws IOException, StudyImporterException {
        final InputStream inputStream = ResourceUtil.asInputStream(resource, null);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        for (JsonNode table : StudyImporterForMetaTable.collectTables(config)) {
            StudyImporterForMetaTable.importTable(interactionListener, tableFactory, table);
        }

    }


}