package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.taxon.GlobalNamesService;
import org.eol.globi.taxon.GlobalNamesSources;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.util.CSVTSVUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GlobalNamesServiceTest {

    @Test
    public void createTaxaListFromNameList() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.ITIS);
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        service.findTermsForNames(Arrays.asList("1|Homo sapiens", "2|Ariopsis felis"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                assertNotNull(nodeId);
                foundTaxa.add(taxon);
            }
        });

        assertThat(foundTaxa.size(), is(2));
    }

    @Test
    public void createTaxaListFromNoNameList() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.ITIS);
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        service.findTermsForNames(Arrays.asList("1|Donald duck", "2|Mickey mouse"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                assertNotNull(nodeId);
                assertThat(nameType, is(NameType.NONE));
                foundTaxa.add(taxon);
            }
        });

        assertThat(foundTaxa.size(), is(2));
    }

    @Test
    public void createTaxaListFromNameListNCBI() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.NCBI);
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        service.findTermsForNames(Collections.singletonList("1|Prunus persica L."), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                assertNotNull(nodeId);
                foundTaxa.add(taxon);
            }
        });

        assertThat(foundTaxa.size(), is(1));
        assertThat(foundTaxa.get(0).getExternalId(), is(TaxonomyProvider.NCBI.getIdPrefix() + "3760"));
    }

    @Test
    public void createTaxaListFromNameWithSpecialCharacter() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.IF);
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        service.findTermsForNames(Collections.singletonList("4594386|Epichloë"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                assertNotNull(nodeId);
                foundTaxa.add(taxon);
            }
        });

        assertThat(foundTaxa.size(), is(1));
    }

    @Test
    public void createTaxaListFromLongNameList() throws PropertyEnricherException {
        String response = "4594759|Botryobasidium pruinatum|4594240|Bombus (Bombus) magnus|4595520|Botrytis byssoidea|4595788|Botrytis galanthina|4595533|Botrytis cinerea|4594249|Bombus (Bombus) cryptarum|4594257|Bombus (Psithyrus) campestris|4594269|Bombus (Melanobombus) lapidarius|4594279|Bombus (Pyrobombus) pratorum|4594789|Botryobasidium subcoronatum|4594786|Phlebia tremellosa|4594272|Bombus (Psithyrus) rupestris|4594528|Botryobasidium candicans|1019686|Lilium speciosum|4594282|Bombus (Psithyrus) sylvestris|4594792|Schizopora paradoxa|4594803|Botryobasidium vagum|4594289|Bombus (Pyrobombus) jonellus|4595070|Botryosphaeria quercuum|4594815|Choisya ternata|4594300|Bombus (Psithyrus) vestalis|4594812|Botryodiplodia fraxini|4594553|Botryobasidium conspersum|4594438|Atriplex|4595207|Gladiolus murielae|4594955|Brachypodium|4594441|Bothynoderes affinis|4595222|Botryotinia fuckeliana|4595478|Botrytis aclada|4594704|Botryobasidium intertextum|4594975|Botryosphaeria melanops|4594460|Beta|4594970|Botryosphaeria hyperborea|4594471|Heterocerus flexuosus|4594468|Botryandromyces heteroceri|4594465|Heterocerus fenestratus|4594721|Botryobasidium laeve|4594476|Augyles maritimus|4594988|Botryosphaeria obtusa|4594218|Bombus (Psithyrus)|4594230|Bombus (Bombus) lucorum|4594483|Botryobasidium aureum|4595507|Botrytis anthophila|4594746|Botryobasidium obtusisporum|4595003|Rhamnus|4594233|Bombus (Psithyrus) bohemicus|4594375|Persicaria alpina|4595652|Botrytis elliptica|4595649|Lilium candidum|4594382|Bostrychonema alpestre|4595151|Caltha palustris|3904181|Botrytis croci|4594389|Botanophila cuspidata|4594386|Epichloë|4595154|Botryotinia calthae|4595409|Botryotinia polyblastis|4595921|Asparagus officinalis|4594395|Botanophila dissecta|4595163|Botryotinia convoluta|4594904|Botryosphaeria festucae|4594404|Botanophila laterella|4595168|Botryotinia draytonii|4594670|Botryobasidium danicum|4594413|Botanophila latifrons|4595435|Botryotinia porri|4595432|Allium vineale|4594423|Botanophila phrenione|4594679|Botryobasidium ellipsosporum|4594418|Botanophila lobata|4595453|Botryotinia squamosa|4595450|Allium triquetrum|4594823|Botryohypochnus isabellinus|4595079|Botryosphaeria rhodorae|4594818|Botryodiplodia theobromae|4595843|Galanthus nivalis|4594318|Andrena (Zonandrena) flavipes|4594315|Bombylius discolor|4594312|Andrena (Melandrena) cineraria|4433042|Botryophialophora marina|4006631|Botrytis hyacinthi|4595357|Botryotinia globosa|4595354|Allium ursinum|4595096|Botryosphaeria ribis|4594342|Bopyrina ocellata|4594339|Hippolyte varians|4595363|Botryotinia narcissicola|4595360|Narcissus pseudonarcissus|4595105|Botryosphaeria stevensii|4594350|Spirontocaris|4595118|Botryosporium pulchrum|4594861|Botryosphaeria abietina|4594347|Bopyroides hippolytes|4594358|Bopyrus squillarum|4595126|Mentha arvensis|4594355|Palaemon serratus|4594867|Botryosphaeria dothidea|4594864|Rosa canina agg.|4595121|Cirsium palustre|4594363|Bostrichonema polygoni|4595640|Crocus";
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        assertAtLeastFortyFound(response, foundTaxa, Collections.singletonList(GlobalNamesSources.ITIS));
        assertThat(foundTaxa.size() > 40, is(true));
    }

    @Test
    public void createTaxaListFromLongNameList2() throws PropertyEnricherException {
        String response = "4475460" +
                "|Zilora ferruginea|4474176|Xerocomus communis|4474947|Xylota segnis|4475203|Zaraea fasciata|4474447|Xylaria filiformis|4" +
                "475477|Zodion|4474960|Xylota sylvarum|4475216|Zaraea lonicerae|4474973|Xylota tarda|4475480|Iberis|4475225|Zelleromyces " +
                "stephensii|4475238|Zenillia libatrix|4474978|Xylota xanthocnema|4475490|Zodion cinereum|4474479|Xylaria guepinii|4475503" +
                "|Zoellneria eucalypti|4474473|Xylaria friesii|4475241|Archiearis notha|4474987|Xylotachina diluta|4474484|Xylaria hypoxy" +
                "lon|4474992|Xyphosia miliaria|4475512|Zoellneria rosarum|4475142|Carabus (Megodontus) violaceus|4475654|Zygorhizidium me" +
                "losirae|4474369|Xyela julii|4475139|Zaira cinerea|4475651|Aulacoseira italica subsp. subarctica|4475662|Kirchneriella ob" +
                "esa|4475147|Carabus (Morphocarabus) monilis|4475659|Zygorhizidium parvum|4475157|Pterostichus (Platysma) niger|4474641|X" +
                "yleborus dryographus|4474386|Xyela longula|4475667|Kirchneriella|4475164|Zalerion arboricola|4474911|Xylohypha ortmansia" +
                "e|4474395|Xylaplothrips fuliginosus|4475430|Galerucella|4474658|Xylechinus pilosus|4475181|Zalerion maritima|4474671|Xyl" +
                "etinus longitarsis|4474920|Xylohypha pinicola|4474420|Xylaria carpophila|4475445|Zignoëlla morthieri|4474934|Xylophaga p" +
                "raestans|4475703|Zygospermella striata|4474929|Xylophaga dorsalis|4475698|Zygospermella insignis|4475455|Zignoëlla slapt" +
                "onensis|4474681|Xylobolus frustulatus|4474426|Xylaria|4475194|Zaraea aenea|4475450|Zignoëlla rhytidodes|4475078|Zabrus t" +
                "enebrioides|4475585|Zwackhiomyces dispersus|4474307|Xerula radicata|4474829|Xylohypha ferruginosa|4474824|Xylocoris (Xyl" +
                "ocoris) formicetorum|4475350|Zeugophora turneri|4475095|Zacladus exiguus|4475607|Zwackhiomyces sphinctrinoides|4474320|X" +
                "estobium rufovillosum|4475602|Zwackhiomyces lacustris|4475100|Zacladus geranii|4474334|Xestophanes potentillae|4474591|X" +
                "yleborinus saxesenii|4475610|Leptogium turgidum|1632955|Xylota|4475111|Phillyrea latifolia|4475619|Clauzadea metzleri|44" +
                "74604|Xyleborus dispar|4474351|Xiphydria prolongata|4474858|Xylohypha nigrescens|4475114|Zaghouania phillyreae|4475371|Z" +
                "euzera pyrina|4475639|Zygogloea gemellipara|9397|Halictus|4475634|Zygiobia carpini|4474364|Xyela curva|4475644|Zygophial" +
                "a jamaicensis|4475269|Zeugophora flavicollis|4475264|Zenobiana prismatica|4475521|Zoopage thamnospira|4475535|Zoophthora" +
                " anglica|4475530|Zoophagus insidians|816095|Phillyrea latifolia|4475544|Zoophthora radicans|4474790|Xylocleptes bispinus" +
                "|4475558|Zoothamnion arbuscula|4474273|Xerula caussei|4475043|Pisum sativum var. sativum|4474798|Cryptolestes ferrugineu" +
                "s|4474795|Xylocoris (Proxylocoris) galactinus|4475563|Zopfia rhizophila|4475572|Zopfiella erostrata|4474806|Xylocoris (X" +
                "ylocoris) cursitans|4474545|Xylaria oxyacanthae|4474803|Bitoma crenata|4474809|Rhizophagus|4474298|Xerula pudens";
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        assertAtLeastFortyFound(response, foundTaxa, Collections.singletonList(GlobalNamesSources.ITIS));
        assertThat(foundTaxa.size() > 40, is(true));
    }

    @Test
    public void createTaxaListFromLongNameList3() throws PropertyEnricherException {
        String response = "4471364" +
                "|Valsella amphoraria|4471621|Velutina plicatilis|4472135|Verticillium|4472128|Lecanora albescens|4471618|Hydrozoa|447238" +
                "6|Vibrissea guernisacii|4472140|Verticillium albo-atrum|4471373|Valsella clypeata|4471624|Styela coriacea|4471627|Veluti" +
                "na velutina|4471382|Valsella polyspora|4471632|Venturia carpophila|4471888|Venturia maculiformis|347923|Scilla|4472154|V" +
                "erticillium catenulatum|4471387|Valsella salicis|4471396|Vankya ornithogali|4470375|Bellevalia|4472167|Verticillium dahl" +
                "iae|4471905|Venturia minuta|4470127|Ustilago maydis|4471919|Venturia populina|4471914|Venturia palustris|4471147|Valsa i" +
                "ntermedia|4471156|Valsa laurocerasi|1052452|Salix matsudana|4471422|Vararia gallica|4472185|Verticillium insectorum|4470" +
                "534|Valsa ambiens|4472070|Veronaea botryosa|4470529|Valsa abrupta|4471555|Velutarina rufo-olivacea|4472079|Veronaea cari" +
                "cis|4472084|Veronaea carlinae|4472089|Veronaea parvispora|4471322|Valsaria insitiva|4470305|Ustilago tritici|4471086|Val" +
                "sa cypri|4471855|Venturia macularis|4472111|Verpa conica|4471081|Valsa ceuthospora|4472116|Verrucaria conturmatula|44703" +
                "20|Scilla sardensis|4470323|Ustilago vaillantii|4472371|Vespula (Vespula) austriaca|4472381|Vibrissea flavovirens|447212" +
                "1|Verrucaria latericola|4470330|Muscari botryoides|4272191|Valsa|4471355|Valsella adhaerens|4470470|Valdensia heterodoxa" +
                "|4472007|Venturia saliciperda|4472263|Physarum compressum|4472268|Physarum leucopus|4470217|Elytrigia juncea|4472277|Ste" +
                "monitis axifera|4471260|Valsaria anserina|4471516|Vasates pedicularis|4470493|Gaultheria|4471519|Acer saccharinum|447229" +
                "3|Vesiculomyces citrinus|4471527|Vasates retiolatus|4472288|Verticillium|4471265|Valsaria cincta|4471522|Vasates quadrip" +
                "edes|4471779|Venturia crataegi|4470504|Valsa abietis|4471784|Venturia ditricha|4471540|Vasates rigidus|4471793|Venturia " +
                "fraxini|4472061|Venturiocistella ulicicola|4471550|Velutarina juniperi|4471806|Venturia geranii|4472056|Venturiocistella" +
                " heterotricha|4471545|Vascellum pratense|4471941|Venturia pyrina|4471936|Venturia potentillae|4470414|Ustilentyloma bref" +
                "eldii|4470927|Valsa auerswaldii|4472200|Verticillium nubilum|4472213|Verticillium psalliotae|4471190|Valsa sordida|44711" +
                "85|Valsa pini|4471441|Climbing plants|4470431|Animalia|4470424|Ustilentyloma fluitans|4470936|Valsa ceratosperma|4471448" +
                "|Vararia ochroleuca|4471705|Venturia cerasi|4472218|Verticillium rexianum|4470438|Utricularia australis|4471974|Venturia" +
                " rumicis|4472230|Ceratiomyxa fruticulosa|4472225|Arcyria nutans|4470441|Crustacea|4471209|Populus balsamifera|4470448|Ut" +
                "ricularia minor|4470194|Ustilago serpens|4471738|Venturia chlorospora|4470459|Diaptomus";
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        assertAtLeastFortyFound(response, foundTaxa, Collections.singletonList(GlobalNamesSources.ITIS));
        assertThat(foundTaxa.size() > 20, is(true));
    }

    @Test
    public void createTaxaListFromLongNameList5() throws PropertyEnricherException {
        String response = "8231805|Aeshna grandis|8231804|Syrphetodes obtusus|8231807|Argia immunda|8231806|Gomphus kurilis|8231801|Accipiter cooperii|8231800|Cladonia macilenta|8231803|Eucalyptus|8231802|Branta canadensis|8231797|Mactridae|8231796|Stereum|8231799|Bryopsida|8231798|Hemigrapsus oregonensis|8231793|Hedera helix|8231792|Trigonospila brevifacies|8231795|Strix varia|8231794|Vinca minor|8231789|Formica obscuripes|8231788|Aphis fabae|8231791|Rubus armeniacus|8231790|Heracleum maximum|8231785|Malus ×domestica|8231784|Sitta canadensis|8231787|Walshomyia|8231786|Juniperus|8231781|Corylus avellana|8231780|Tremella foliacea|8231783|Eucalyptus globulus|8231782|Canis familiaris|8231777|Elymus repens|8231776|Auricularia auricula-judae|8231779|Stereum hirsutum|8231778|Claviceps purpurea|8231773|Equisetum hyemale|8231772|Stamnaria americana|8231775|Oxalis articulata|8231774|Viscum album album|8231769|Puccinia oxalidis|8231768|Oxalis debilis corymbosa|8231771|Gymnosporangium sabinae|8231770|Pyrus communis|8231765|Philaeus chrysops|8231764|Calliphora|8231767|Polistes dominula|8231766|Muscidae|8231761|Acridotheres tristis|8231760|Hemidactylus persicus|8231763|Pandion haliaetus|8231762|Atherinopsis californiensis|8231757|Metacarcinus|8231756|Loxorhynchus grandis|8231759|Aphidoidea|8231758|Nassarius fossatus|8231753|Metacarcinus magister|8231752|Cancer productus|8231755|Enhydra lutris kenyoni|8231754|Octopus|8231749|Cancridae|8231748|Crassadoma gigantea|8231751|Romaleon antennarium|8231750|Carcinus maenas|8231745|Mytilus californianus|8231744|Tresus nuttallii|8231747|Enhydra lutris|8231746|Saxidomus|8231741|Mytilus|8231740|Coluber flagellum testaceus|8231743|Bivalvia|8231742|Enhydra lutris nereis|8231737|Sciurus carolinensis|8231736|Manduca|8231739|Agapostemon splendens|8231738|Larus occidentalis|8231732|Fraxinus|8231833|Lantana|8231832|Gomphrena globosa|8231834|Bidens aristosa|8231829|Cercyonis pegala|8231828|Eriogonum|8231831|Papilio rumiko|8231830|Euphilotes enoptes bayensis|8231825|Felicia|8231824|Ichthyostomum pygmaeum|8231827|Junonia hierta|8231826|Gazania|8231821|Weinmannia racemosa|8231820|Coleosporium tussilaginis|8231823|Knightia excelsa|8231822|Tmesipteris tannensis|8231817|Verbena brasiliensis|8231816|Mantodea|8231819|Senecio minimus|8231818|Tmetolophota steropastis|8231813|Celithemis ornata|8231812|Argia tibialis|8231815|Araneus bicentenarius|8231814|Argiope|8231809|Microstylum morosum|8231808|Orthetrum sabina|8231811|Calopteryx maculata|8231810|Micrathyria hagenii";
        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        assertAtLeastFortyFound(response, foundTaxa, Collections.singletonList(GlobalNamesSources.ITIS));
        assertThat(foundTaxa.size() > 20, is(true));
    }


    @Test
    public void createTaxaListFromLongNameList4() throws PropertyEnricherException {
        List<String> names = namesListWithMaximumOf(100);

        final List<Taxon> foundTaxa = new ArrayList<Taxon>();
        GlobalNamesService service = new GlobalNamesService(Arrays.asList(GlobalNamesSources.values()));

        try {
            service.findTermsForNames(names, new TermMatchListener() {
                @Override
                public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                    assertNotNull(nodeId);
                    foundTaxa.add(taxon);
                }
            });
        } catch (PropertyEnricherException ex) {
            fail("failed to lookup name with id: [" + names + "]");
        }
        assertThat(foundTaxa.size() > 2, is(true));
    }

    public List<String> namesListWithMaximumOf(int i1) {
        String response = "76546|Bromus kalmii|4592968|Bromus commutatus|4593225|Triticum turgidum|4592981|Bromus hordeaceus|4593493|Bolbitius reticulatus var. pluteoides|2857788|Koeleria macrantha|4593502|Bolbitius titubans var. titubans|4592991|Bromus japonicus|4592986|Bromus interruptus|4594022|Boletus luridus var. luridus|4593765|Boletus fechtneri|4572257|Bromus|4593507|Boletopsis perplexa|4593004|Bromus madritensis|4593512|Boletus aereus|4593782|Boletus ferrugineus|4593009|Ceratochloa marginata|4593415|Boidinia permixta|4593157|Poa alpina|4013434|Bromus briziformis|4593166|Poa bulbosa|4592911|Apera spica-venti|4593420|Boidinia peroxydata|4592906|Anthoxanthum odoratum|4593943|Boletus luridiformis var. luridiformis|4593429|Bolacothrips jordani|4593938|Boletus luridiformis var. discolor|4593171|Poa compressa|4593683|Boletus cisalpinus|4593438|Bolbitius lacteus|4592925|Avena sterilis subsp. ludoviciana|4593181|Poa nemoralis|4592920|Avena fatua|4593176|Poa glauca|4013149|Bromus pectinatus|4592934|Avena strigosa|4593186|Poa palustris|4593443|Bolbitius reticulatus|4592943|Briza media|4593199|Puccinellia distans|4593719|Boletus edulis|4593716|Dryas octopetala|4593712|Boletus depilatus|491120|Bromus|4593212|Sesleria caerulea|4593976|Boletus luridus|4593095|Hordeum distichon sensu lato|4592834|Bloxamia leucophthalma|4593090|Hordelymus europaeus|4593858|Boletus impolitus|4013239|Bromus gracillimus|4593100|Hordeum jubatum|4593613|Boletus calopus|4013234|Bromus condensatus|4593110|Hordeum secalinum|4592855|Bloxamia truncata|4594128|Boletus pseudoregius|4593105|Hordeum murinum|4592862|Melastoma affine|4592859|Culicoides|4593883|Boletus legaliae|4594137|Boletus pseudosulphureus|4593378|Boidinia furfuracea|4592865|Artiodactyla|4592878|Agrostis stolonifera|4593909|Boletus luridiformis|4593139|Lolium temulentum|4592881|Blumeria graminis|4592893|Alopecurus myosuroides|4593658|Boletus chrysenteron|4594170|Boletus pulverulentus|4592888|Alopecurus geniculatus|4593144|Milium effusum|4593541|Boletus appendiculatus|4593026|Danthonia decumbens|4594051|Boletus luridus var. rubriceps|4592783|Trichia affinis|4593035|Digitaria sanguinalis|4013544|Bromus riparius|4593040|Elymus caninus|4593054|Festuca gigantea|4593823|Boletus fragrans|4592793|Trichia botrytis|4593049|Festuca arundinacea|4592807|Trichia varia|4594085|Boletus pinophilus|4593059|Festuca heterophylla|4594094|Boletus porosporus|4592812|Diderma effusum|4593068|Festuca pratensis|4593324|Blumeriella jaapii|4593578|Boletus armeniacus|4593832|Boletus immutatus|4592822|Symphyta|4593335|Prunus domestica|4592819|Blondelia nigripes|4593596|Boletus badius non sensu Persoon (1801)|4592829|Bloxamia bohemica|4593085|Holcus mollis|3923392|Boeremia telephii";
        String[] idsNames = CSVTSVUtil.splitPipes(response);
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < idsNames.length && i < i1 * 2; i += 2) {
            names.add(idsNames[i] + "|" + idsNames[i + 1]);
        }
        return names;
    }


    public void assertAtLeastFortyFound(String response, final List<Taxon> foundTaxa, List<GlobalNamesSources> sources) {
        String[] idsNames = CSVTSVUtil.splitPipes(response);
        GlobalNamesService service = new GlobalNamesService(sources);

        List<String> names = new ArrayList<String>();
        for (int i = 0; i < idsNames.length; i += 2) {
            names.add(idsNames[i] + "|" + idsNames[i + 1]);
        }
        try {
            service.findTermsForNames(names, new TermMatchListener() {
                @Override
                public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                    assertNotNull(nodeId);
                    foundTaxa.add(taxon);
                }
            });
        } catch (PropertyEnricherException ex) {
            fail("failed to lookup name with id: [" + names + "]");
        }
    }


    @Test
    public void lookupITIS() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        Map<String, String> props = assertHomoSapiens(service);
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:180092"));
    }

    @Test
    public void lookupITISSynonymFails() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Corizidae");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:108477"));
    }

    @Test
    public void lookupOTT() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.OTT);
        HashMap<String, String> props = new HashMap<>();
        props.put(PropertyAndValueDictionary.NAME, "Arius felis");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("OTT:139650"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
    }

    @Test
    public void lookupITISSynonymSuccess() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Arius felis");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:680665"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
    }

    @Test
    public void lookupNCBI() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.NCBI);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is(" | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is(" | superkingdom |  | kingdom |  |  |  | phylum | subphylum |  |  |  |  |  |  |  |  | class |  |  |  | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    @Test
    public void lookupNCBIBacteria() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.NCBI);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Bacteria");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Bacteria"));

        final String path = enrich.get(PropertyAndValueDictionary.PATH);
        assertThat(path, is(" | Bacteria"));

        final String pathNames = enrich.get(PropertyAndValueDictionary.PATH_NAMES);
        assertThat(pathNames, is(" | superkingdom"));

        final String pathIds = enrich.get(PropertyAndValueDictionary.PATH_IDS);
        assertThat(pathIds, is("NCBI:131567 | NCBI:2"));

        assertThat(StringUtils.split(path, CharsetConstant.SEPARATOR_CHAR).length, is(2));
        assertThat(StringUtils.split(pathIds, CharsetConstant.SEPARATOR_CHAR).length, is(2));
        assertThat(StringUtils.split(pathNames, CharsetConstant.SEPARATOR_CHAR).length, is(2));

        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:2"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    @Test
    public void lookupWoRMS() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Ariopsis felis");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), containsString("Siluriformes | Ariidae | Ariopsis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is(""));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), containsString("order | family | genus"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("WORMS:158709"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), not(containsString("hardhead catfish @en")));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), not(containsString("bagre boca chica @en")));
    }

    @Test
    public void lookupWoRMSCod() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props1 = new HashMap<String, String>();
        props1.put(PropertyAndValueDictionary.NAME, "Gadus morhua");
        Map<String, String> enrich = service.enrich(props1);
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), containsString("Gadiformes | Gadidae | Gadus | Gadus morhua"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
    }

    private Map<String, String> assertHomoSapiens(GlobalNamesService service) throws PropertyEnricherException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        String expectedPath = "Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens";
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is(expectedPath));
        String expectedIds = "ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:914181 | ITIS:179913 | ITIS:179916 | ITIS:179925 | ITIS:180089 | ITIS:943773 | ITIS:943778 | ITIS:943782 | ITIS:180090 | ITIS:943805 | ITIS:180091 | ITIS:180092";
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is(expectedIds));
        String expectedRanks = "Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Subclass | Infraclass | Order | Suborder | Infraorder | Superfamily | Family | Subfamily | Genus | Species";
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is(expectedRanks));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
        return enrich;
    }

    @Test
    public void lookupITISNonExisting() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Donald Duck");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.size(), is(0));
    }

    @Test
    public void lookupITISFish() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService();
        service.setIncludeCommonNames(true);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Ariopsis felis");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Actinopterygii | Teleostei | Ostariophysi | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("ITIS:202423 | ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:161061 | ITIS:161105 | ITIS:162845 | ITIS:163992 | ITIS:164157 | ITIS:639019 | ITIS:680665"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("Kingdom | Subkingdom | Infrakingdom | Phylum | Subphylum | Infraphylum | Superclass | Class | Superorder | Order | Family | Genus | Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:680665"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is("bagre boca chica @Spanish"));
    }

    @Test
    public void lookupGBIF() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.GBIF);
        service.setIncludeCommonNames(true);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Anura");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Anura"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Amphibia | Anura"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:1 | GBIF:44 | GBIF:131 | GBIF:952"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("order"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:952"));
//        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Бесхвостые @ru"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("\u0416\u0430\u0431\u044b @ru"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Frogs @en"));
    }

    @Test
    public void lookupWORMS() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(GlobalNamesSources.WORMS);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertyAndValueDictionary.NAME, "Anura");
        Map<String, String> enrich = service.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Anura"));
    }

    @Test
    public void lookupMultipleSources() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(Arrays.asList(GlobalNamesSources.GBIF, GlobalNamesSources.ITIS));
        final List<Taxon> taxa = new ArrayList<Taxon>();
        service.findTermsForNames(Collections.singletonList("Homo sapiens"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                taxa.add(taxon);
            }
        });

        assertThat(taxa.size(), is(2));

    }

    @Test
    public void lookupSimilar() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(Arrays.asList(GlobalNamesSources.GBIF, GlobalNamesSources.ITIS));
        final List<Taxon> taxa = new ArrayList<Taxon>();
        service.findTermsForNames(Collections.singletonList("Zyziphus mauritiana"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                taxa.add(taxon);
                assertThat(nameType, is(NameType.SIMILAR_TO));
            }
        });

        assertThat(taxa.size() > 1, is(true));

    }

    @Test
    public void lookupNCBIPrune() throws PropertyEnricherException {
        GlobalNamesService service = new GlobalNamesService(Arrays.asList(GlobalNamesSources.NCBI));
        final List<Taxon> taxa = new ArrayList<>();
        service.findTermsForNames(Collections.singletonList("Klebsiella pneumoniae"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                taxa.add(taxon);
                assertThat(nameType, is(NameType.SAME_AS));
            }

        });

        assertThat(taxa.size(), is(1));
        assertThat(taxa.get(0).getPath(), endsWith ("Klebsiella | Klebsiella pneumoniae"));

    }

}
