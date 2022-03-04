package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ResultFormatterGraphStreamingTest {

    @Test
    public void toGraphStreaming() throws IOException {
        String result = ResultFormatterJSONTestUtil.getNewInteractionResults();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        String actual = IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name());

        assertThat(actual, is(ensureLineFeedAndNewline("example.gs")));
    }

    @Test
    public void toAriopsisFelisDietToGraphStreaming() throws IOException {
        String result = "{\"results\":[{\"columns\":[\"source_taxon_name\",\"interaction_type\",\"target_taxon_name\"],\"data\":[{\"row\":[\"Ariopsis felis\",\"preysOn\",[\"Harengula jaguana\",\"Opisthonema oglinum\",\"Anchoa hepsetus\"]],\"meta\":[null,null,null,null,null]},{\"row\":[\"Ariopsis felis\",\"eats\",[\"Macoma\",\"Alosa pseudoharengus\",\"Nereis\",\"Animalia\",\"Mya arenaria\",\"Chrysaora quinquecirrha\",\"Ctenophora\",\"Bacteria\",\"Anchoa mitchilli\",\"Actinopterygii\",\"Brevoortia tyrannus\",\"Crassostrea virginica\",\"Polychaeta\",\"Micropogonias undulatus\",\"Leiostomus xanthurus\",\"Ariopsis felis\",\"Alosa sapidissima\",\"Trinectes maculatus\",\"Hesionidae\",\"Gnathostomata\",\"Osteichthyes\",\"Brachyura\",\"Ascidiacea\",\"Bivalvia\",\"Caridea\",\"Mollusca\",\"Crustacea\",\"Arthropoda\",\"Echinodermata\",\"Pisces\",\"Penaeus\",\"Annelida\",\"Diptera\",\"Menippe adina\",\"Callinectes sapidus\",\"Ericthonius punctatus\",\"Hymenoptera\",\"Palaemonetes vulgaris\",\"Xanthidae\",\"Alpheus heterochaelis\",\"Dyspanopeus texanus\",\"Scissurella costata\",\"Callianassidae\",\"Mysidae\",\"Nemertea\",\"Ostreidae\",\"Uca\",\"Decapoda\",\"Aegathoa oculata\",\"Pectinariidae\",\"Brevoortia patronus\",\"Amphipoda\",\"Insecta\",\"Araneae\",\"Gastropoda\",\"Syngnathinae\",\"Squilla empusa\",\"Ensis megistus\",\"Terebellidae\",\"Isopoda\",\"Ampelisca\",\"Anguilliformes\",\"Petrolisthes armatus\",\"Ergasilus\",\"Ischadium recurvum\",\"Teleostei\",\"Cirripedia\",\"Glycera\",\"Ovalipes\",\"Albunea paretii\",\"Opheliidae\",\"Armandia maculata\",\"Pleuronectiformes\",\"Lumbrineridae\",\"Nudibranchia\",\"Glyceridae\",\"Anaitides\",\"Maldanidae\",\"Euceramus praelongus\",\"Bigelowina biminiensis\",\"Goniadidae\",\"Leucosiidae\",\"Lestrigonus\",\"Listriella\",\"Phyllodocidae\",\"Rudilemboides\",\"Cephalopoda\",\"Pinnixa\",\"Ogyrides\",\"Lumbrineris coccinea\",\"Polynoidae\",\"Mysidopsis furca\",\"Synchelidium\",\"Isolda pulchella\",\"Cyclaspis\",\"Protomystides\",\"Holothuroidea\",\"Collodes\",\"Albunea\",\"Photis\",\"Lucifer faxoni\",\"Processa\",\"Axiidae\",\"Anchialina\",\"Oxyurostylis\",\"Monoculodes nyei\",\"Amphideutopus\",\"Portunus\",\"Anomura\",\"Squilla\",\"Goniadella\",\"Squilla edentata\",\"Glycera robusta\",\"Callinectes\",\"Diopatra\",\"Portunus gibbesii\",\"Glycinde\",\"Onuphidae\",\"Trachypenaeus\",\"Squillidae\",\"Glycera americana\",\"Sicyonia dorsalis\",\"Speocarcinus lobatus\",\"Megalops\",\"Rimapenaeus similis\",\"Diopatra cuprea\",\"Goniada\",\"Sipuncula\",\"Portunidae\",\"Bothidae\",\"Engraulidae\",\"Anchoa hepsetus\",\"Speocarcinus\",\"Ampeliscidae\",\"Hippomedon\",\"Spionidae\",\"Sthenolepis\",\"Goniada littorea\",\"Callianassa\",\"Gammaridae\",\"Copepoda\",\"Mysida\",\"Aplousobranchia\",\"Priapulida\",\"Emerita\",\"Nematoda\",\"Crangonidae\",\"Macrobrachium\",\"Coleoptera\",\"Arachnida\",\"Hydrozoa\",\"Lophogastrida\",\"Pleocyemata\",\"Stomatopoda\",\"Cumacea\",\"Urochordata\",\"Syngnathus\",\"Bairdiella chrysoura\",\"Orthopristis chrysoptera\",\"Rhithropanopeus harrisii\",\"Chironomidae\",\"Penaeidae\",\"Farfantepenaeus duorarum\",\"Zygoptera\",\"Adoxomyia dahlii\",\"Formicidae\",\"Hyalella azteca\",\"Rangia cuneata\",\"Americamysis almyra\",\"Dorosoma\",\"Corophium\",\"Mytilopsis leucophaeata\",\"Hydrophilidae\",\"Orthoptera\",\"Melita\",\"Dytiscidae\",\"Cyathura polita\",\"Nereididae\",\"Texadina sphinctostoma\",\"Cladocera\",\"Gammarus\",\"Ceratopogonidae\",\"Lestes\",\"Odonata\",\"Belostoma\",\"Xanthoidea\",\"Mulinia lateralis\",\"Lepidopa\",\"Anthozoa\",\"Pagurus\",\"Pectinaria\",\"Oenonidae\",\"Sedentaria\",\"Farfantepenaeus aztecus\",\"Latreutes parvulus\",\"Nassarius acutus\",\"Polyonyx gibbesi\",\"Olivella\",\"Heterocrypta granulata\",\"Ampelisca verrilli\",\"Callinectes similis\",\"Persephona\",\"Hyperiidea\",\"Cnidaria\",\"Mysidopsis\",\"Paguridae\",\"Labidocera aestiva\",\"Pagurus longicarpus\",\"Plecoptera\",\"Litopenaeus setiferus\",\"Molgula\",\"Acetes americanus\",\"Hydrachnidia\",\"Eucarida\",\"Calanoida\",\"Loligo\",\"Temora turbinata\",\"Pterygota\",\"Upogebia\",\"Subeucalanus pileatus\",\"Austinixa cristata\",\"Raninoides louisianensis\",\"Sigambra tentaculata\",\"Teuthida\",\"Dendrobranchiata\",\"Deuterostomia\",\"Brevoortia\",\"Paguroidea\",\"Mugilidae\",\"Lepidophthalmus louisianensis\",\"Panopeus\",\"Stenotomus\",\"Sicyonia\",\"Menidia\",\"Palaemonidae\",\"Ostracoda\",\"Anchoa\",\"Chordata\",\"Dyspanopeus sayi\",\"Lagodon rhomboides\",\"Cynoscion nebulosus\",\"Palaemonetes pugio\",\"Myrophis punctatus\"]],\"meta\":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null]}]}],\"errors\":[]}";

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        String actual = IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name());

        assertThat(actual, is(ensureLineFeedAndNewline("ariopsisFelisDiet.gs")));
    }

    @Test
    public void toEnhydraLutrisDietObservationsToGraphStreaming() throws IOException {
        String result = IOUtils.toString(getClass().getResourceAsStream("enhydraLutrisDietObservationsCypher.json"), StandardCharsets.UTF_8);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        String actual = IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name());

        assertThat(actual, is(ensureLineFeedAndNewline("enhydraLutrisDietObservations.gs")));
    }

    @Test
    public void toEnhydraDietObservationsToGraphStreaming() throws IOException {
        String result = IOUtils.toString(getClass().getResourceAsStream("enhydraDietObservationsCypher.json"), StandardCharsets.UTF_8);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        String actual = IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name());

        //IOUtils.write(actual, new FileOutputStream("/home/jorrit/proj/globi/eol-globi-data/eol-globi-rest/src/test/resources/org/eol/globi/server/util/enhydraDietObservations.gs"), StandardCharsets.UTF_8);

        assertThat(actual,
                is(ensureLineFeedAndNewline("enhydraDietObservations.gs")));
    }

    public static String ensureLineFeedAndNewline(String resourceName) throws IOException {
        String value = IOUtils.toString(ResultFormatterGraphStreamingTest.class.getResourceAsStream(resourceName), StandardCharsets.UTF_8);
        return StringUtils.replace(value, "\r{0,1}\n", "\r\n");
    }

    @Test(expected = ResultFormattingException.class)
    public void throwOnErrorStreaming() throws IOException {
        String result = RequestHelperTest.getErrorResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);
        } catch (ResultFormattingException ex) {
            assertThat(ex.getMessage(), is("failed to format incoming stream"));
            assertThat(ex.getCause().getMessage(), is("failed to retrieve results"));
            throw ex;
        }

    }

}