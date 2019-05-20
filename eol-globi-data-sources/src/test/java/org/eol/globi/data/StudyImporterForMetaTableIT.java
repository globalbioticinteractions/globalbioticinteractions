package org.eol.globi.data;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class StudyImporterForMetaTableIT {

    @Test
    public void parseNaturalHistoryMuseum() throws IOException, StudyImporterException {
        final Class<StudyImporterForMetaTable> clazz = StudyImporterForMetaTable.class;
        final String name = "test-meta-globi-nhm.json";
        final URL resource = clazz.getResource(name);
        Assert.assertNotNull(resource);

        final JsonNode config = new ObjectMapper().readTree(clazz.getResourceAsStream(name));
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        dataset.setConfig(config);
        final List<JsonNode> tables = StudyImporterForMetaTable.collectTables(dataset);
        assertThat(tables.size(), is(1));
        JsonNode firstTable = tables.get(0);
        String bibliographicCitation = firstTable.get("dcterms:bibliographicCitation").asText();
        assertThat(bibliographicCitation, containsString("NHM Interactions Bank. https://doi.org/10.5519/0060767"));

        String resourceUrl = firstTable.get("url").asText();
        // see https://github.com/jhpoelen/eol-globi-data/issues/266
        //assertThat(resourceUrl, is("http://data.nhm.ac.uk/dataset/82e807f0-6273-4f19-be0a-7f7558442a25/resource/1f64e2cf-d738-4a7c-9e81-a1951eac635f/download/output.csv"));
        assertThat(firstTable.get("headerRowCount").asInt(), is(1));
        assertThat(firstTable.has("tableSchema"), is(true));
        assertThat(firstTable.has("null"), is(true));
        assertThat(firstTable.get("tableSchema").has("columns"), is(true));
    }


    @Test
    public void importAll() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = properties -> links.add(properties);
        final StudyImporterForMetaTable.TableParserFactory tableFactory = (config, dataset) -> {
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

            return CSVTSVUtil.createLabeledCSVParser(CSVTSVUtil.createCSVParse(IOUtils.toInputStream(firstFewLines, StandardCharsets.UTF_8)));
        };


        final String baseUrl = "https://raw.githubusercontent.com/globalbioticinteractions/AfricaTreeDatabase/master";
        final String resource = baseUrl + "/globi.json";

        importAll(interactionListener, tableFactory, baseUrl, resource);
        assertThat(links.size(), is(9));
    }

    @Test
    public void importREEMWithStaticCSV() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = properties -> links.add(properties);

        final StudyImporterForMetaTable.TableParserFactory tableFactory = (config, dataset) -> {
            String firstFewLines = "Hauljoin,\" Pred_nodc\",\" Pred_specn\",\" Prey_nodc\",\" Pred_len\",\" Year\",\" Month\",\" day\",\" region\",\" Pred_name\",\" Prey_Name\",\" Vessel\",\" Cruise\",\" Haul\",\" Rlat\",\" Rlong\",\" Gear_depth\",\" Bottom_depth\",\" Start_hour\",\" Surface_temp\",\" Gear_temp\",\" INPFC_Area\",\" Stationid\",\" Start_date\",\" Prey_sz1\",\" Prey_sex\"\n" +
                    "11012118.0,8791030401.0,5.0,9999999998.0,53.0,1994.0,7.0,11.0,AI,\"Pacific cod Gadus macrocephalus\",\"Rocks \",95.0,199401.0,148.0,51.43,178.81999999999999,222.0,228.0,11.0,0.63,0.41999999999999998,542.0,118-11,\"1994-07-11 00:00:00\",3.0,\n" +
                    "11012118.0,8791030401.0,8.0,9999999998.0,53.0,1994.0,7.0,11.0,AI,\"Pacific cod Gadus macrocephalus\",\"Rocks \",95.0,199401.0,148.0,51.43,178.81999999999999,222.0,228.0,11.0,0.63,0.41999999999999998,542.0,118-11,\"1994-07-11 00:00:00\",3.0,\n" +
                    "11012118.0,8791030401.0,9.0,9999999998.0,58.0,1994.0,7.0,11.0,AI,\"Pacific cod Gadus macrocephalus\",\"Rocks \",95.0,199401.0,148.0,51.43,178.81999999999999,222.0,228.0,11.0,0.63,0.41999999999999998,542.0,118-11,\"1994-07-11 00:00:00\",13.0,\n" +
                    "11012118.0,8791030401.0,9.0,9999999998.0,58.0,1994.0,7.0,11.0,AI,\"Pacific cod Gadus macrocephalus\",\"Rocks \",95.0,199401.0,148.0,51.43,178.81999999999999,222.0,228.0,11.0,0.63,0.41999999999999998,542.0,118-11,\"1994-07-11 00:00:00\",3.0,\n";

            return CSVTSVUtil.createLabeledCSVParser(CSVTSVUtil.createCSVParse(IOUtils.toInputStream(firstFewLines, StandardCharsets.UTF_8)));
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
    public void importPHIBaseWithStaticCSV() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        final InteractionListener interactionListener = properties -> links.add(properties);

        final StudyImporterForMetaTable.TableParserFactory tableFactory = (config, dataset) -> {
            String firstFewLines = ",,Record 1,PHI:3,Uniprot,P26215,EMBL,AAA79885,MVAYALTSMLLSAGALVAAAPSGLDARDGCTFTDAATAIKNKASCSNIVISGMTVPAGTTLDLTGLKSGATVTFQGTTTFGYKEWEGPLISVSGTNIKVVGASGHTIDAAGQKWWDGKGSNGGKTKPKFFYAHSLTTSSISGLNIKNTPVQAFSINGVTGLTLDRITIDNSAGDSAGAHNTDAFDIGSSSGITISNANIKNQDDCVAINSGSDIHVTNCQCSGGHGVSIGSVGGRKDNTVKGVVVSGTTIANSDNGVRIKTISGATGSVSDITYENITLKNIAKYGIVIEQDYLNGGPTGKPTTGVPITGVTLKNVAGSVTGSGTEIYVLCGKGSCSGWNWSGVSITGGKKSSSCLNVPSGASC,no data found,SB111,PGN1,no data found,,,,,no,5017,Bipolaris zeicola,no data found,SB111,Leaf spot,Monocots,4577,Zea mays (related: maize),no data found,,,no data found,Endopolygalacturonase,GO:0004650,GO,no data found,unaffected pathogenicity,no data found,no,no,no,no data found,,,,,no data found,pectin,,,,,,Gene disruption,,,MU; JA,2152162,PubMed,,,,Expression during all infection stages. pathogen formerly called Cochliobolus carbonum teleomorph name,Scott-Craig et al.,1990,,,,,,,,,,,,,,,,,,,,,,,\n" +
                    ",,Record 2,PHI:7,Uniprot,P22287,EMBL,CAA42824,MKLSLLSVELALLIATTLPLCWAAALPVGLGVGLDYCNSSCTRAFDCLGQCGRCDFHKLQCVH,no data found,race 5,AVR9,no data found,,,,,no,5499,Passalora fulva,no data found,no data found,Leaf mould,Eudicots,4081,Solanum lycopersicum (related: tomato),no data found,,,no data found,Effector protein,no data found,no data found,no data found,effector (plant avirulence determinant),no data found,no data found,no data found,no data found,no data found,,,,,no data found,,,,,,,Gene deletion; gene complementation; biochemical evidence,,,MU,1799694,PubMed,,,,,Van Kan et al.,1991,,,,,,,,,,,,,,,,,,,,,,,\n" +
                    ",,Record 3,PHI:12,Uniprot,Q01886,EMBL,AAA33023,MTMPHHSSGPAKDSPLCRFPPFPGGNPVFTNIRREKVNFQLPPPDHALLAAAWAVLLRLYTGHVKTCFESATSDQEANLVTYEARDSDTLQTIVLRGACVSSTAEEKAGLRDLNTAVVRTTVSIDSWTDEMLQDKIAALLQPGKEIVLFQTPSGCVLVYMQSFMSAMEVKNVSSTLTYIMSSDPDKTAIRNLSISPRDLAQIMRWNDRKLKSERTNLVYDLFSARAHEQDANMAIDAWDGRMSYTELERVSSTWARQLQKQGISQGSWVLFCFEKSRLAVVSMIAILKAGGVCVPIDPRYPVERIRDIIRTTNATIALVGAGKTAALFKSADTAVQTIDITKDIPHGLSDTVVQSNTKIDDPAFGLFTSGSTGVPKCIVVTHSQICTAVQAYKDRFGVTSETRVLQFSSYTFDISIADTFTALFYGGTLCIPSEEDRMSNLQDYMVSVRPNWAVLTPTVSRFLDPGVVKDFISTLIFTGEASREADTVPWIEAGVNLYNVYGPAENTLITTATRIRKGKSSNIGYGVNTRTWVTDVSGACLVPVGSIGELLIESGHLADKYLNRPDRTEAAFLSDLPWIPNYEGDSVRRGRRFYRTGDLVRYCDDGSLICVGRSDTQIKLAGQRVELGDVEAHLQSDPTTSQAAVVFPRSGPLEARLIALLVTGNKDGTPHNQQSLPKPAFAQCPPDLVKYATSSLQQRLPSYMVPSVWLGIDFLPMSVSGKLDRAVLQDQLESLSPSDYAEILGTTGLEVDPGGAASSVASDSDLRDMNDDSLLLTACSRVLNLPAGKISYSQSFIHAGGDSITAMQVSSWMKRFTGKRIGVKDLLVSPSISTAASCIKSAQDGSRNFVAVRPGQRIPVSPIQKLFFQTAEASKSWNHYHQSFLFRIDQPIKPQTIEDAISLVMQRHPMLQARFERTEEGDWYQYIPIDVERRASVEVIGSLSTDDREAAMLRARQSIDLTEGPLIRCQLFNNNVDEASRLFFVVIHHAVVDLVSWRIIMEELEAHLATDSTPDRGEAYQESVPFLAWCQVQAEAVKDIPVDRTVPLIPKIPTADFGYWGLKHDENVYGNTVERKIPLGHSITEDLLYKCHDSLHTKTIDVLLAAVLVSFRRSFLDRPVPAVFNEGHGREPGGEDAVDLSRTVGWFTTISPVYVPEVSPGDILDVVRRVKDYRWATPNNGFDYFSTKYLTQSGIKLFEDHLPAEILFNYEGRYQAMESEQTVLKPESWHAGEASKDQDPGLRRFCLFEISTAVLPDGQLHLTCSWNKNMRHQGRIRLWLDTLLPAAIGEIVSSLALASPQLTLSDVELLRLYDYSSLDILKKSILSIPAVQTLDDLEGVYPGSPMQDALFLSQSKSQDGAYEVDFTWRVATSLQNSQPAVDIGCLVEAWKDTVALHAALRTVILESSLPATGILHQVVLRSHDPDIVILDVRDVTAAITILDSYPPPTEEGIALIKRPPHRLLICTTIEGSVLIKFQVNHLVFDGMSTDKIIQDLSKAYTCRHSNKLPDHSESKLHDGTYGNRPTKPPLAEFIRYIRDPQRKQDSINYWKNALRGATTCSFPPLFDQITSEKAMPRQSWASVPIPLCVDSKELSKTLANLGITMSTMFQTVWAIVLRIYSQNGQSVFGYLTSGRDAPVDGIDSAVGNFIAMLVCFFDFDDDGVHTVADMARKIHNASANSISHQACSLAEIQDALGLSTSTPLFNTAFTYLPKRPTNVKAGEPEHHLCFEELSMSDPTEFDLTLFVEPTQESNEVSAHLDFKLSYISQAYATSIASTVAHILSELVHDPYRALNTLPIVSEHDTAIIRSWNDHLFPPATECIHETFSRKVVEHPQREAICSWDGSLTYAELSDLSQRLSIHLVSLGIKVGTKIPICFEKSMWTIVTILAVVQAGGVFVLLEPGHPESRLSGIIKQVQAELLLCSPATSRMGALQNISTQMGTEFKIVELEPEFIRSLPLPPKPNHQPMVGLNDDLYVVFTSGSTGVPKGAVATHQAYATGIYEHAVACGMTSLGAPPRSLQFASYSFDASIGDIFTTLAVGGCLCIPREEDRNPAGITTFINRYGVTWAGITPSLALHLDPDAVPTLKALCVAGEPLSMSVVTVWSKRLNLINMYGPTEATVACIANQVTCTTTTVSDIGRGYRATTWVVQPDNHNSLVPIGAVGELIIEGSILCRGYLNDPERTAEVFIRSPSWLHDLRPNSTLYKTGDLVRYSADGKIIFIGRKDTQVKMNGQRFELGEVEHALQLQLDPSDGPIIVDLLKRTQSGEPDLLIAFLFVGRANTGTGNSDEIFIATSTSSLSEFSTVIKKLQDAQRAMEVLPLFMVPQAYIPIEGGIPLTAAGKIDRRMLRKLCEPFNRNDLISFTSKALSTSVKDAETTDTVEDRLARIWEKVLGVKGVGRESDFFSSGGNSMAAIALRAEAQRSGFTLFVADIFTNPRLADMAKLFSHGQSVSPSSSTLRTKVPISSLQKRSSGLQTAAPVSNGSPVRRCQKENIIDCPVAFEYEEGPSDTQLKEASRICGISSRSIEDVFPCTPMQEALVALSLIPGAQASYALHAAFELRPGLDRNRFRSAWESTVKAQPILRSRIISGSNGSSVVVTSATDSIPQLDVSGLDTFLEQQLQVGFAPGAPLFRLAFVYSKADDCDYFVISAHHAIYDGWSLNLIWSQVLALYTNGELPPPGPSFKHFARNLNLVQSKLDSEDFWRKLLVKPDQESFRFPDVPVGHKPATRCTTNFHFPFSMQSKIGTTANTCINAAWAITLAQYSSNKTVNFGVTLWGRDFPMIDIEHMTGPTIVTVPRQVNVIPESSVAEFLQDLQKSLAVVLPHQHLGLHRIQALGPIARQACDFSTLLVVNHGSSISWSELEAADIVPVPLRSSDLYAYPMVVEVENASSDTLDIRVHSDPDCIEVQLLERLMEQFGHNLQTLCRAASFDPGKRIAELMDDTATTHLRTLFSWNSRVKDSPDVAAIAVHKLLEETAQSQPAESAIVAHDGQLSYMQMDRCADVLARQIRKTNMISAQSPFVCIHLLRSATAVVSMLAVLKAGGAFMPVDISQPRSRLQNLIEESGAKLVLTLPESANALATLSGLTKVIPVSLSELVQQITDNTTKKDEYCKSGDTDPSSPAYLLYTSGTSGKPKGVVMEHRAWSLGFTCHAEYMGFNSCTRILQFSSLMFDLSILEIWAVLYAGGCLFIPSDKERVNNLQDFTRINDINTVFLTPSIGKLLNPKDLPNISFAGFIGEPMTRSLIDAWTLPGRRLVNSYGPTEACVLVTAREISPTAPHDKPSSNIGHALGANIWVVEPQRTALVPIGAVGELCIEAPSLARCYLANPERTEYSFPSTVLDNWQTKKGTRVYRTGDLVRYASDGTLDFLGRKDGQIKLRGQRIELGEIEHHIRRLMSDDPRFHEASVQLYNPATDPDRDATVDVQMREPYLAGLLVLDLVFTDEVMGIPCTSLTSANTSENLQTLVTELKKSLRGVLPHYMVPLHFVAVSRLPTGSSGKLDHAFVRACLRELTAPLDGNFPKVEQVLTTNESVLRQWWGTVLAMDPHSIQRGDDFFSLGGSSISAMRLVGLARSSGHKLQHEDIFMCPRLADMAGQISFVQEASVSPTTSPTIKFDLLDDCEVDEVIDHILPQLDMNKELIEDVYPCTPLQESLMAATARHGEAYTMIQSITVLASQLAQLKKAMDVVFRDFEVLRTRIALGPSQQALQVVVKHEELSWESFPSIQSFKDHFYRSLGYGKPLARLAVITQALDTKQPISHGTREARTKNSQDTVMVVVGAHHSIYDAHVLSMIWRRLYREFIGSQADGILEAETSRSEGVVPFKSYVEKLLRGKDNDESLLFWKEKLRGVSSSQFPPASWPRVLEHQPSATQTLITKVSLPTSSRKKLGATVATVAYAAWALTIAHYTADPDVVFGATLSGRETMAGSISHPESIAGPTIITVPLRIIIDFQTVVSDFLSTLQKDIVRAAYFGQMMGLNSIAHIDNDCRDACGFKSIIVVQVPDEGENHDGRAANPFQMSLESIGHFPAPLVVEVEQSESTDVLIRMAYDPVLVPEKLAHFISDTFTTTMSNLSAANPKAKVESIPALSEAHLAELDVTCPEWILGKAKDEKIRTESHQCLQDLVCRRAQQSPNSQAIDSWDGSISYHELDGLSSILAEHLSQLGVRPEAPVCLLFEKSKWAVVAMIGIIKAGGCFVPLDPSYPHERLEHIISETGSSVIVTSAAYSKLCLSLSVRGIVCDGSVFSSTKKPLPSTADSPPSFSVRPNQAAYILFTSGSTGKPKGVVMEHHSVCSALIALGKRMGLGPQSRVLQFNSYWFDVMLLDIFGTLVYGGCLCIPKEEQRMSNLSGWVQKFKVNTMLLSTSVSRLMQPADTPSLETLCLTGEAVLQSDVDRWAPKLHLIAGYGPTETCIMSVSGELTPSSPANLIGKPVSCQAWVINPLKETELAPYGATGELYIQGPTVARGYLHDDVLTSKAFIVDPQWLTGYKTNENQWSRRAYKTGDLVFWGPQSNLYYVRRKDSSQVKIRGQRVELAEIEEVIRQHIPPDVTVCVDLLSSDDQNTRIILGAVLGIGDRALGGPEDLEVIGYMDDLKSHIIPALEASLPHHMIPEAYVPFVQLPTLGSGKLDRKTVRRVAGPLAFSLPQASARHPNQPTVTHTQKLLRQLWCKILPQLDESAVNKQDNFLGIGGDSIAAIKLVALLRQHGISLAVAEIFTRPTLEAMSSLIDEHNFVVSHAGILSDVTRNTSGVMRQTTNLIAGRHSMAVEKSRECDNSTLPCTEYQQMFLAGTEAFTGAHSAQFIFRLPEKIDLDRLQAAFDHCADWYPNLRTQIHKDADTGRLLHDISPIGVKVPWSCHYSDDLNTVLSHDKKFPPGLDGPLHRVTIMRHRDPTESMLVWTLNHAAYDAWSLRMMLEHITEAYANPDYEPSYSLGWTAFVLHTENTKEASRSFWSSYLSDVKPARLMFNYNLVSNPRQDRLYEARINIPKRVLSQATAATVLLAGLTLLVARVCDTRDVILAHLLTGRTLPLAGIENCPGPTITKVPLRIPLMDQDLVTLELDSVAKKITAELMRVMPHEHSGLSAIREFIPQAEGTTTSSGKFHAGSVLGRLPLDLVIHPKGGLDLLGKHGLGLQNEGFRLVAPPSGGLSMECALVDDDDDKRSDTISVDVSVLWDQRAATQEDVIELVHSLQGIFTKRNLAASICLMYK,no data found,SB111,HTS1,no data found,,,,,no,5017,Bipolaris zeicola,no data found,SB111,Leaf spot,Monocots,4577,Zea mays (related: maize),no data found,,,no data found,Cylic Peptide Synthase,\"GO:0009405, IMP\",GO,no data found,loss of pathogenicity,no data found,no data found,no data found,no data found,no data found,,,,,no data found,,,,,,,Gene disruption,,,TKB; JA,11607305,PubMed,,,,pathogen formerly called Cochliobolus carbonum teleomorph name,Panaccione et al.,1992,,,,,,,,,,,,,,,,,,,,,,,\n" +
                    ",,Record 4,PHI:14,Uniprot,P0C017,EMBL,AAB09711,MAPRKTVGILGGGQLGRMLTHPAALLGIPLLILDSGSFTPAKQTLLPPPSHSHPDGPFTSEPHIRKLASACDILTVEIEHVNADVLEAVEKEGLCEVQPSPKTIRLIQNKYDQKKYLAEKGVAVAPFEELPANPTEEDFKAIAGRLGLPLMLKAKTLAYDGRGNSPLKSTSSEDIQASLKFLGDRPLYAEGWAPFVKEVAVMVVRNKEGEVQSYDAVETIHRESILRVCLAPLRGERGVNQRARELAEKAVGHLEGAGIFGVEMFLMPDGELLLNEIAPRPHNSGHHTIEACLTSQFENHLRAILSLPLGSTALRVPSAAMVNILGASSTMDAIDKMADNALTVPGAAVHLYGKAESRKARKMGHITVTAESDAELNERLRTLLFAQPDAHADWIDLIAPPSPAPAHSHPKPLVGIIMGSDSDLPVMHPATKILEKFGVPYELTITSAHRTPERMVKYAKTAAGRGLRAIIAGAGGAAHLPGMVASETSLPVIGVPVKASVLDGVDSLYSIVQMPRGIPCATVGINNSTNAALLAIRILGTSVPALNKATEEYSKALEEEVLAKVDILEEEGWDKYIERLKK,no data found,clinical isolate,ADE2,no data found,,,,,no,5207,Cryptococcus neoformans,235443,H99 (serotype A),Cryptococcosis,Nematodes,6239,Caenorhabditis elegans (related: roundworm),no data found,,,no data found,Phosphoribosylaminoimidazole carboxylase,\"GO:0009405, IMP\",GO,no data found,reduced virulence,no data found,no data found,no data found,no data found,no data found,,,,,no data found,,,,,,,Gene disruption; gene complementation,,,TKB; KHK,12438649,PubMed,10.1073/pnas.232568599,,,,Perfect et al.,1993,,,,,,,,,,,,,,,,,,,,,,,\n" +
                    ",,Record 5,PHI:14,Uniprot,P0C017,EMBL,AAB09711,MAPRKTVGILGGGQLGRMLTHPAALLGIPLLILDSGSFTPAKQTLLPPPSHSHPDGPFTSEPHIRKLASACDILTVEIEHVNADVLEAVEKEGLCEVQPSPKTIRLIQNKYDQKKYLAEKGVAVAPFEELPANPTEEDFKAIAGRLGLPLMLKAKTLAYDGRGNSPLKSTSSEDIQASLKFLGDRPLYAEGWAPFVKEVAVMVVRNKEGEVQSYDAVETIHRESILRVCLAPLRGERGVNQRARELAEKAVGHLEGAGIFGVEMFLMPDGELLLNEIAPRPHNSGHHTIEACLTSQFENHLRAILSLPLGSTALRVPSAAMVNILGASSTMDAIDKMADNALTVPGAAVHLYGKAESRKARKMGHITVTAESDAELNERLRTLLFAQPDAHADWIDLIAPPSPAPAHSHPKPLVGIIMGSDSDLPVMHPATKILEKFGVPYELTITSAHRTPERMVKYAKTAAGRGLRAIIAGAGGAAHLPGMVASETSLPVIGVPVKASVLDGVDSLYSIVQMPRGIPCATVGINNSTNAALLAIRILGTSVPALNKATEEYSKALEEEVLAKVDILEEEGWDKYIERLKK,no data found,clinical isolate,ADE2,no data found,,,,,no,5207,Cryptococcus neoformans,235443,H99 (serotype A),Cryptococcosis,Rabbits & hares,9986,Oryctolagus cuniculus (related: rabbit),no data found,,,no data found,Phosphoribosylaminoimidazole carboxylase,\"GO:0009405, IMP\",GO,no data found,loss of pathogenicity,no data found,no data found,no data found,no data found,no data found,,,,,no data found,,,,,,,Gene disruption; gene complementation,,,TKB; KHK,12438649,PubMed,10.1073/pnas.232568599,,,,Perfect et al.,1993,,,,,,,,,,,,,,,,,,,,,,,\n" +
                    ",,Record 6,PHI:15,Uniprot,P52751,EMBL,AAA20128,MFSLKTVVLALAAAAFVQAIPAPGEGPSVSMAQQKCGAEKVVSCCNSKELKNSKSGAEIPIDVLSGECKNIPINILTINQLIPINNFCSDTVSCCSGEQIGLVNIQCTPILS,no data found,Guy-11,MPG1,no data found,,,,,no,318829,Magnaporthe oryzae,no data found,Guy11,Rice blast,Monocots,4530,Oryza sativa (related: rice),no data found,,,no data found,Hydrophobin,\"GO:0009405, IMP; GO:0051701\",GO,no data found,reduced virulence,no data found,Yes,no data found,no data found,no data found,,,,,no data found,,,,,,,Gene deletion,,,MU,8312740,PubMed,10.1105/tpc.5.11.1575,,,,Talbot et al.,1993,,,,,,,,,,,,,,,,,,,,,,,\n" +
                    ",,Record 7,PHI:17,Uniprot,Q00663,EMBL,CAA43678,MATIFLFTKNVFIALAFALFAQGLTIPDGIEKRTDKVVSLDFTVIRKPFNATAHRLIQKRSDVPTTLINEGPSYAADIVVGSNQQKQTVVIDTGSSDLWVVDTDAECQVTYSGQTNNFCKQEGTFDPSSSSSAQNLNQDFSIEYGDLTSSQGSFYKDTVGFGGISIKNQQFADVTTTSVDQGIMGIGFTAVEAGYNLYSNVPVTLKKQGIINKNAYSCDLNSEDASTGKIIFGGVDNAKYTGTLTALPVTSSVELRVHLGSINFDGTSVSTNADVVLDSGTTITYFSQSTADKFARIVGATWDSRNEIYRLPSCDLSGDAVVNFDQGVKITVPLSELILKDSDSSICYFGISRNDANILGDNFLRRAYIVYDLDDKTISLAQVKYTSSSDISAL,no data found,ATCC750,ACP,no data found,,,,,no,5482,Candida tropicalis,5482,ATCC 750,Candidiasis,Rodents,10090,Mus musculus (related: house mouse),no data found,,,no data found,Acid proteinase,\"GO:0009405, IMP; GO:0004190\",GO,no data found,reduced virulence,no data found,no data found,no data found,no data found,no data found,,,,,no data found,,,,,,,Gene disruption; gene complementation,,,TKB,7983570,PubMed,,,,,Togni et al.,1994,,,,,,,,,,,,,,,,,,,,,,,\n" +
                    ",,Record 8,PHI:18,Uniprot,Q00363,EMBL,CAA69643,MHYTTLLLSTLLVGTALAQPTNPPAKTPKKAPKTQPYNPCKPQEVIDTKCMGPKDCLYPNPDSCTTYIQCVPLDEVGNAKPVVKPCPKGLQWNDNVGKKWCDYPNLSTCPVKTPQPKPKKGGVGGKKASVGHPGY,no data found,Race 5,AVR4,no data found,,,,,no,5499,Passalora fulva,no data found,Multiple alleles from numerous strains,Leaf mould,Eudicots,4081,Solanum lycopersicum (related: tomato),no data found,,,no data found,Effector protein,no data found,no data found,no data found,effector (plant avirulence determinant),no data found,no data found,no data found,no data found,no data found,,,,,no data found,,,,,,,Biochemical analysis; gene mutation: characterised; gene complementation,,,TKB,8114941,PubMed,,Nature.� 1994 Jan 27;367(6461):384-6,,Expression during biotrophic growth,Joosten et al,1994,,,,,,,,,,,,,,,,,,,,,,,\n";

            return CSVTSVUtil.createLabeledCSVParser(CSVTSVUtil.createCSVParse(IOUtils.toInputStream(firstFewLines, StandardCharsets.UTF_8)));
        };

        final String baseUrl = "https://raw.githubusercontent.com/phi-base/data/master";
        final String resource = baseUrl + "/globi.json";
        importAll(interactionListener, tableFactory, baseUrl, resource);

        assertThat(links.size(), is(7));

        final Map<String, String> firstLine = links.get(0);
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002556"));
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("pathogenOf"));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_ID), is("NCBITaxon:4081"));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Solanum lycopersicum (related: tomato)"));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_ID), is("NCBITaxon:5499"));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Passalora fulva"));
        assertThat(firstLine.get(StudyImporterForTSV.REFERENCE_URL), is("https://www.ncbi.nlm.nih.gov/pubmed/1799694"));

        assertThat(links.get(3).get(StudyImporterForTSV.REFERENCE_DOI), is("10.1073/pnas.232568599"));
    }


    @Test
    public void importAPSNET() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = properties -> links.add(properties);

        final String baseUrl = "https://raw.githubusercontent.com/globalbioticinteractions/apsnet-common-names-plant-diseases/master";
        final String resource = baseUrl + "/globi.json";
        importAll(interactionListener, new StudyImporterForMetaTable.TableParserFactoryImpl(), baseUrl, resource);

        assertThat(links.size()> 10000, is(true));

        final Map<String, String> firstLine = links.get(0);
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_ID), startsWith("http://purl.obolibrary.org/obo/RO_"));
        assertNotNull(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_NAME));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_ID), is("NCBITaxon:13547"));
        assertNotNull(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_ID), is("NCBITaxon:1441629"));
        assertNotNull(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME));
        assertNotNull(firstLine.get(StudyImporterForTSV.REFERENCE_URL));
        assertNotNull(firstLine.get(StudyImporterForTSV.REFERENCE_CITATION));
    }

    @Test
    public void importGandhi() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = links::add;

        final String baseUrl = "https://raw.githubusercontent.com/globalbioticinteractions/gandhi2009/master";
        final String resource = baseUrl + "/globi.json";
        importAll(interactionListener, new StudyImporterForMetaTable.TableParserFactoryImpl(), baseUrl, resource);

        assertThat(links.size()> 0, is(true));
        final Map<String, String> firstLine = links.get(0);
        assertThat(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_ID), startsWith("http://purl.obolibrary.org/obo/RO_"));
        assertNotNull(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_NAME));
        assertThat(firstLine.get(StudyImporterForTSV.REFERENCE_CITATION), is(not(nullValue())));
        assertThat(firstLine.get(StudyImporterForTSV.REFERENCE_DOI), is(not(nullValue())));
        assertThat(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME), is(not(nullValue())));
        assertThat(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is(not(nullValue())));

    }

    @Test
    public void importNHMStatic() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = links::add;

        final StudyImporterForMetaTable.TableParserFactory tableFactory = (config, dataset) -> {
            String firstFewLines = "\"InteractionID\",\"InteractionURL\",\"Species1UUID\",\"Species1Name\",\"Species1LifeCycleStage\",\"Species1OrganismPart\",\"Species1Status\",\"InteractionType\",\"InteractionOntologyURL\",\"Species2UUID\",\"Species2Name\",\"Species2LifeCycleStage\",\"Species2OrganismPart\",\"Species2Status\",\"LocationUUID\",\"LocationName\",\"LocationCountryName\",\"ISO2\",\"Importance\",\"InteractionRecordType\",\"Reference\",\"ReferenceDOI\",\"Reference Page\",\"Notes\"\n" +
                    "\"4bee827f-c9f5-4c0e-9db3-e40a6e4d8008\",\"http://phthiraptera.info/node/94209\",\"c8faa033-237b-40b9-9b76-d9e7fcff9238\",\"Menacanthus alaudae\",\"\",\"\",\"\",\"ectoparasite of\",\"http://purl.obolibrary.org/obo/RO_0002632\",\"e275d77c-e993-4de0-981f-b3f39fd4da9b\",\"Acanthis flavirostris\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"310\",\"[REF: Palma, Price & Hellenthal, 1998:310]\"\n" +
                    "\"80e66e7c-75db-467f-9a89-a11f94d58eb3\",\"http://phthiraptera.info/node/94210\",\"fe5b2e50-b414-41d9-840d-189e732b2ea5\",\"Ricinus fringillae flammeae\",\"\",\"\",\"\",\"ectoparasite of\",\"http://purl.obolibrary.org/obo/RO_0002632\",\"f26a1199-c0bb-4d7c-a511-2fe6284c5378\",\"Acanthis flammea flammea\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"Self citation to checklist added. Requires page number.\"\n" +
                    "\"001ee8aa-dbab-43b8-9137-a61565ccf41b\",\"http://phthiraptera.info/node/94211\",\"ee17d179-9f60-4198-ac49-dc9dab3ae529\",\"Brueelia sibirica\",\"\",\"\",\"\",\"ectoparasite of\",\"http://purl.obolibrary.org/obo/RO_0002632\",\"f26a1199-c0bb-4d7c-a511-2fe6284c5378\",\"Acanthis flammea flammea\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"Self citation to checklist added. Requires page number.\"\n" +
                    "\"d0929673-2f4c-49ec-877f-116e74ea360e\",\"http://phthiraptera.info/node/94212\",\"46084bc3-cfbf-4e01-96f8-5ecb50bc5ff9\",\"Ricinus fringillae\",\"\",\"\",\"\",\"ectoparasite of\",\"http://purl.obolibrary.org/obo/RO_0002632\",\"2027cf09-f15d-4c2b-be28-9cb00fabf308\",\"Acanthis flammea\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"204\",\"[REF: Rheinwald, 1968:204]\"\n";
            return CSVTSVUtil.createLabeledCSVParser(CSVTSVUtil.createCSVParse(IOUtils.toInputStream(firstFewLines, StandardCharsets.UTF_8)));
        };

        final String baseUrl = "https://raw.githubusercontent.com/globalbioticinteractions/natural-history-museum-london-interactions-bank/master";
        final String resource = baseUrl + "/globi.json";
        importAll(interactionListener, tableFactory, baseUrl, resource);

        assertThat(links.size(), is(4));

        for (Map<String, String> firstLine : links) {
            assertNotNull(firstLine.get(StudyImporterForTSV.INTERACTION_TYPE_NAME));
            assertNotNull(firstLine.get(StudyImporterForTSV.TARGET_TAXON_ID));
            assertNotNull(firstLine.get(StudyImporterForTSV.TARGET_TAXON_NAME));
            assertNotNull(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_ID));
            assertNotNull(firstLine.get(StudyImporterForTSV.SOURCE_TAXON_NAME));
        }
    }

    static public void importAll(InteractionListener interactionListener, StudyImporterForMetaTable.TableParserFactory tableFactory, String baseUrl, String resource) throws IOException, StudyImporterException {
        final InputStream inputStream = ResourceUtil.asInputStream(resource);
        final JsonNode config = new ObjectMapper().readTree(inputStream);
        final Dataset dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        dataset.setConfig(config);
        for (JsonNode table : StudyImporterForMetaTable.collectTables(dataset)) {
            StudyImporterForMetaTable.importTable(interactionListener, tableFactory, table, new DatasetImpl(null, URI.create(baseUrl)), null);
        }

    }


}