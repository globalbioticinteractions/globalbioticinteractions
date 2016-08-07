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
import static org.junit.Assert.assertNotNull;
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


        final String baseUrl = "https://raw.githubusercontent.com/globalbioticinteractions/AfricaTreeDatabase/master";
        final String resource = baseUrl + "/globi.json";

        importAll(interactionListener, tableFactory, baseUrl, resource);
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

        final String baseUrl = "https://raw.githubusercontent.com/globalbioticinteractions/noaa-reem/master";
        final String resource = baseUrl + "/globi.json";
        importAll(interactionListener, tableFactory, baseUrl, resource);

        assertThat(links.size(), is(12));

        final Map<String, String> firstLine = links.get(0);
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_ID), is(nullValue()));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Rocks"));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_ID), is("NODC:8791030401"));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Pacific cod Gadus macrocephalus"));
        assertThat(firstLine.get(StudyImporterForMetaTable.EVENT_DATE), startsWith("1994-07-11"));
        assertThat(firstLine.get(StudyImporterForMetaTable.LATITUDE), is("51.43"));
        assertThat(firstLine.get(StudyImporterForMetaTable.LONGITUDE), is("178.81999999999999"));
    }


    @Test
    public void importAPSNET() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = new InteractionListener() {

            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                links.add(properties);
            }
        };

        final String baseUrl = "https://raw.githubusercontent.com/globalbioticinteractions/apsnet-common-names-plant-diseases/master";
        final String resource = baseUrl + "/globi.json";
        importAll(interactionListener, new StudyImporterForMetaTable.TableParserFactoryImpl(), baseUrl, resource);

        assertThat(links.size(), is(9659));

        final Map<String, String> firstLine = links.get(0);
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_ID), startsWith("http://purl.obolibrary.org/obo/RO_"));
        assertNotNull(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_NAME));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_ID), is(nullValue()));
        assertNotNull(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_ID), is(nullValue()));
        assertNotNull(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME));
        assertNotNull(firstLine.get(StudyImporterForTSV.REFERENCE_URL));
        assertNotNull(firstLine.get(StudyImporterForTSV.REFERENCE_CITATION));
    }

    @Test
    public void importNHMStatic() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = new InteractionListener() {

            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                links.add(properties);
            }
        };

        final StudyImporterForMetaTable.TableParserFactory tableFactory = new StudyImporterForMetaTable.TableParserFactory() {

            public CSVParse createParser(JsonNode config) throws IOException {
                String firstFewLines = "\"InteractionID\",\"InteractionURL\",\"Species1UUID\",\"Species1Name\",\"Species1LifeCycleStage\",\"Species1OrganismPart\",\"Species1Status\",\"InteractionType\",\"InteractionOntologyURL\",\"Species2UUID\",\"Species2Name\",\"Species2LifeCycleStage\",\"Species2OrganismPart\",\"Species2Status\",\"LocationUUID\",\"LocationName\",\"LocationCountryName\",\"ISO2\",\"Importance\",\"InteractionRecordType\",\"Reference\",\"ReferenceDOI\",\"Reference Page\",\"Notes\"\n" +
                        "\"f38baed7-002b-4ac1-a113-17d04d969172\",\"http://phasmida.myspecies.info/node/338\",\"f8fab31d-0b94-49a6-861d-682952aa912b\",\"Hermagoras sigillatus\",\"\",\"\",\"\",\"eats\",\"http://purl.obolibrary.org/obo/RO_0002470\",\"88fff3e3-8090-4392-9a48-c2c2d45c72df\",\"Ixora\",\"\",\"\",\"\",\"72f79476-c914-4ee2-8deb-7d4f8cb49f71\",\"Borneo\",\"\",\"\",\"\",\"\",\"F.  Seow-Choen, A Taxonomic Guide to the Stick Insects of Borneo. Kota Kinabalu: Natural History Publications (Borneo), 2016.\",\"\",\"290\",\"\"\n" +
                        "\"da5d7396-a570-4850-a54b-eafea5983078\",\"http://phasmida.myspecies.info/node/337\",\"f8fab31d-0b94-49a6-861d-682952aa912b\",\"Hermagoras sigillatus\",\"\",\"\",\"\",\"eats\",\"http://purl.obolibrary.org/obo/RO_0002470\",\"1550c7a9-16f4-4001-a8a0-19ddf50bf152\",\"Stachytarpheta indica\",\"\",\"\",\"\",\"72f79476-c914-4ee2-8deb-7d4f8cb49f71\",\"Borneo\",\"\",\"\",\"\",\"\",\"F.  Seow-Choen, A Taxonomic Guide to the Stick Insects of Borneo. Kota Kinabalu: Natural History Publications (Borneo), 2016.\",\"\",\"290\",\"\"\n" +
                        "\"00f31e25-afd3-4c24-a572-786db7b1b5ab\",\"http://phasmida.myspecies.info/node/336\",\"f8fab31d-0b94-49a6-861d-682952aa912b\",\"Hermagoras sigillatus\",\"\",\"\",\"\",\"eats\",\"http://purl.obolibrary.org/obo/RO_0002470\",\"70cbb822-afb3-4300-a2f5-e3c6917cd144\",\"Rubus moluccanus\",\"\",\"\",\"\",\"72f79476-c914-4ee2-8deb-7d4f8cb49f71\",\"Borneo\",\"\",\"\",\"\",\"\",\"F.  Seow-Choen, A Taxonomic Guide to the Stick Insects of Borneo. Kota Kinabalu: Natural History Publications (Borneo), 2016.\",\"\",\"290\",\"\"\n" +
                        "\"739f6a75-3f68-4e76-8be5-62f74d77cf89\",\"http://phasmida.myspecies.info/node/335\",\"dfd90fa3-a1ca-4d55-8e7a-32d980af3cd3\",\"Hermagoras matangensis\",\"\",\"\",\"\",\"eats\",\"http://purl.obolibrary.org/obo/RO_0002470\",\"1550c7a9-16f4-4001-a8a0-19ddf50bf152\",\"Stachytarpheta indica\",\"\",\"\",\"\",\"72f79476-c914-4ee2-8deb-7d4f8cb49f71\",\"Borneo\",\"\",\"\",\"\",\"\",\"F.  Seow-Choen, A Taxonomic Guide to the Stick Insects of Borneo. Kota Kinabalu: Natural History Publications (Borneo), 2016.\",\"\",\"288\",\"\"\n" +
                        "\"7081688b-6e36-4b84-b843-5ddd519de2ff\",\"http://phthiraptera.info/node/81842\",\"ac5107a5-699a-4766-beeb-b1a60f3cb20c\",\"Schizophthirus graphiuri\",\"\",\"\",\"\",\"ectoparasite of\",\"\",\"ad71cce0-408c-415b-958b-9456e4147470\",\"Graphiurus kelleni\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n" +
                        "\"85852351-67ae-4974-baf6-5e82bd93be1c\",\"http://phthiraptera.info/node/81843\",\"04dcab2e-c4d1-441f-863a-412f681b4232\",\"Schizophthirus gliris\",\"\",\"\",\"\",\"ectoparasite of\",\"\",\"80668c3d-cda2-4591-82de-6ca90537b125\",\"Myoxus glis\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n" +
                        "\"e03d291d-1869-4324-bbf5-599074a5239d\",\"http://phthiraptera.info/node/81844\",\"18feb94e-6801-4013-b75e-a6853ecdd447\",\"Schizophthirus dryomydis\",\"\",\"\",\"\",\"ectoparasite of\",\"\",\"b0e26119-26a9-4b0b-a0ff-6f4a1de2197b\",\"Dryomys nitedula\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n" +
                        "\"3710cc00-018b-4023-aa79-26085e76a9f7\",\"http://phthiraptera.info/node/81845\",\"2f8cbf09-c5d1-402d-a6b7-3c145fd277d4\",\"Schizophthirus aethogliris\",\"\",\"\",\"\",\"ectoparasite of\",\"\",\"edfb5863-3328-4ae1-a624-8d126447381b\",\"Graphiurus hueti\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n" +
                        "\"8e3effa1-bdf3-4bfa-b4ea-2c4f413df8e5\",\"http://phthiraptera.info/node/81846\",\"66114a3a-16a7-45b0-8cb3-25bbe2215851\",\"Sathrax durus\",\"\",\"\",\"\",\"ectoparasite of\",\"\",\"9d9da32a-1bbe-448b-a979-c3dbad41d14d\",\"Tupaia glis\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n";

                return new LabeledCSVParser(new CSVParser(IOUtils.toInputStream(firstFewLines)));
            }
        };

        final String baseUrl = "https://raw.githubusercontent.com/globalbioticinteractions/natural-history-museum-london-interactions-bank/master";
        final String resource = baseUrl + "/globi.json";
        importAll(interactionListener, tableFactory, baseUrl, resource);

        assertThat(links.size(), is(9));

        for (Map<String, String> firstLine : links) {
            assertNotNull(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_NAME));
            assertNotNull(firstLine.get(StudyImporterForTSV.TARGET_TAXON_ID));
            assertNotNull(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME));
            assertNotNull(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_ID));
            assertNotNull(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME));
            assertNotNull(firstLine.get(StudyImporterForTSV.REFERENCE_URL));
            assertNotNull(firstLine.get(StudyImporterForTSV.REFERENCE_CITATION));
        }
    }

    @Test
    public void importDapstromWithStaticCSV() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = new InteractionListener() {

            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                links.add(properties);
            }
        };

        final StudyImporterForMetaTable.TableParserFactory tableFactory = new StudyImporterForMetaTable.TableParserFactory() {

            public CSVParse createParser(JsonNode config) throws IOException {
                String firstFewLines = "HAUL ID,Year,Date,Sea,Ices division,Predator Latin name,Predator common name,Pooled,Mean length of predator,Prey Latin name,Prey common name,Prey group,Predator ID,Number of stomachs,Prey Length,Minimum number\n" +
                        "PORT-ERIN-1919-A1,1919,06/08/1919,Irish Sea,VIIa,SCOMBER SCOMBRUS,(EUROPEAN) MACKEREL,y,,ACARTIA CLAUSI,ACARTIA CLAUSI,Copepod ,PORT-ERIN-1919-A1/MAC-1,6,,310\n" +
                        "CIROL03-86-31,1986,18/03/1986,Celtic Sea,VIIj,SCOMBER SCOMBRUS,(EUROPEAN) MACKEREL,n,40.8,CRUSTACEA,MARINE CRUSTACEANS,Crustacean,CIROL03-86-31\\MAC-1007926,1,,1\n" +
                        "CIROL03-86-31,1986,18/03/1986,Celtic Sea,VIIj,SCOMBER SCOMBRUS,(EUROPEAN) MACKEREL,n,39.4,CRUSTACEA,MARINE CRUSTACEANS,Crustacean,CIROL03-86-31\\MAC-1007933,1,,1\n" +
                        "CIROL03-86-31,1986,18/03/1986,Celtic Sea,VIIj,SCOMBER SCOMBRUS,(EUROPEAN) MACKEREL,n,43.4,CRUSTACEA,MARINE CRUSTACEANS,Crustacean,CIROL03-86-31\\MAC-1007936,1,,1\n";

                return new LabeledCSVParser(new CSVParser(IOUtils.toInputStream(firstFewLines)));
            }
        };

        final String baseUrl = "https://raw.githubusercontent.com/jhpoelen/Dapstrom-test/master";
        final String resource = baseUrl + "/globi.json";
        importAll(interactionListener, tableFactory, baseUrl, resource);

        assertThat(links.size(), is(8));

        final Map<String, String> firstLine = links.get(0);
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("eats"));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_ID), is(nullValue()));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("ACARTIA CLAUSI"));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_ID), is(nullValue()));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("SCOMBER SCOMBRUS"));
        assertThat(firstLine.get(StudyImporterForMetaTable.EVENT_DATE), startsWith("1919-01-01"));
    }

    static public void importAll(InteractionListener interactionListener, StudyImporterForMetaTable.TableParserFactory tableFactory, String baseUrl, String resource) throws IOException, StudyImporterException {
        final InputStream inputStream = ResourceUtil.asInputStream(resource, null);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        for (JsonNode table : StudyImporterForMetaTable.collectTables(config)) {
            StudyImporterForMetaTable.importTable(interactionListener, tableFactory, table, baseUrl);
        }

    }


}