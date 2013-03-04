package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class StudyImporterForBioInfoTest extends GraphDBTestCase {

    public static final String RELATIONS_STRING = "DonorTax_id\tRecipTax_id\tTrophicRel_Id\tRecipStage\txisRestricted\tRelate_id\tDonorStage\tisRelationshipUncertain\tSeason\txisForeignRecord\txisGBRecord\txisRare\txisMajor\tisIndoorRecord\txisMinor\tDonorState\tRecipState\tDonorPart\tRecipPart\tSymptoms\tisDonorUncertain\tisRecipientUncertain\tDonorPartFrequencyCode4\tDonorPartGeographyCode4\tRecipStageFrequencyCode4\tRecipStageGeographyCode4\tRelationFrequencyCode4\tRelationGeographyCode4\t\n" +
            "43913\t43912\t43902\t\"mycelium\"\tFalse\t43916\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"\"\t\"\"\t\"stem\"\t\"\"\t\"causes witches' broom. Infected shoots are pale yellow, thick and short and grow vertically upwards with short, thick, spirally-arranged leaves\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\n" +
            "3011\t43917\t43902\t\"\"\tFalse\t43918\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"\"\t\"\"\t\"leaf (petiole)\"\t\"\"\t\"\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\n" +
            "3011\t43920\t43902\t\"larva\"\tFalse\t43921\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"\"\t\"\"\t\"leaf (petiole)\"\t\"\"\t\"\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\n" +
            "3011\t107544\t43902\t\"larva\"\tFalse\t43923\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"\"\t\"\"\t\"leaf\"\t\"\"\t\"\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\n" +
            "3011\t1625\t43902\t\"\"\tFalse\t43925\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"live\"\t\"\"\t\"leaf\"\t\"\"\t\"\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\n" +
            "3011\t12958\t43902\t\"\"\tFalse\t43926\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"\"\t\"\"\t\"leaf\"\t\"\"\t\"\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\n" +
            "3011\t43927\t43902\t\"\"\tFalse\t43928\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"\"\t\"\"\t\"leaf\"\t\"\"\t\"\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\n" +
            "465\t43930\t43902\t\"\"\tFalse\t43931\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"\"\t\"\"\t\"leaf\"\t\"\"\t\"\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\n" +
            "464\t32122\t43902\t\"\"\tFalse\t43933\t\"\"\tFalse\t\"\"\tFalse\tFalse\tFalse\tFalse\tFalse\tFalse\t\"\"\t\"\"\t\"leaf\"\t\"\"\t\"\"\tFalse\tFalse\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"";

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporter importer = new StudyImporterFactory(new ParserFactoryImpl(), nodeFactory).createImporterForStudy(StudyLibrary.Study.BIO_INFO);
        // limit the nubmer of line to be imported to make test runs reasonably fast
        importer.setImportFilter(new ImportFilter() {

            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 500;
            }
        });
        Study study = importer.importStudy();

        assertThat(study.getTitle(), is("BIO_INFO"));
        Taxon acer = nodeFactory.findTaxon("Acer");
        assertNotNull(acer);
        assertThat(acer.getExternalId(), is("bioinfo:3011"));
        Taxon taxon = nodeFactory.findTaxon("Dasineura tympani");
        assertNotNull(taxon);
        assertThat(taxon.getExternalId(), is("bioinfo:107544"));
        Taxon taxon1 = nodeFactory.findTaxon("Phyllocoptes acericola");
        assertNotNull(taxon1);
        assertThat(taxon1.getExternalId(), is("bioinfo:43927"));
        Taxon taxon2 = nodeFactory.findTaxon("Aceria eriobia");
        assertNotNull(taxon2);
        assertThat(taxon2.getExternalId(), is("bioinfo:32122"));
    }

    @Test
    public void taxaParsing() throws StudyImporterException, NodeFactoryException, IOException {
        Map<Long, String> taxaMap = buildTaxaMap();
        assertThat(taxaMap.get(1L), is("Biota"));
        assertThat(taxaMap.get(25L), is("Eukaryota"));
    }

    private Map<Long, String> buildTaxaMap() throws IOException, StudyImporterException {
        String taxaString = "Taxon_id\tOwner_id\tLatin80\tEnglish80\tContext\tRecordable\tNBNCode20\tHierarchy80\tTaxonomicNotes\tAuthor\tCriticalSpecies\tShortCut\tAlienState4\tNWebStandardPhotos\tEndemic\tNWebIdentLiterature\tNWebOwnFedOnBy\tNWebImageSubtaxa\tNWebOwnStandardPhotos\tLocalInterestCode4\tImageCode20\tNWebMicrophotos\tNWebLiterature\tNWebMacrophotos\tNWebNotes\tNWebOwnMicrophotos\tNWebOwnMacrophotos\tNWebOwnNotes\tNWebOwnLiterature\tTaxonRank_id\tExposeOnWeb\tNeedsAttention\tNWebOwnFeedsOn\tNWebTrophisms\tBMSCode8\tNWebOwnIdentLiterature\tNWebInfoSubtaxa\tSortCode80\tVeryOldName\t\n" +
                "1\t0\t\"Biota\"\t\"living things\"\tTrue\tTrue\t\"\"\t\"AA\"\t\"NASA's working definition of <quotes>Life<quotes>: a self-sustained chemical system capable of undergoing Darwinian evolution. (Rachel Nowak, writing in New Scientist, 27 July 2002, p13)\"\t\"\"\tFalse\tFals\"NATI\"\t41412\tFalse\t6238\t0\t7452\t0\t\"\"\t\"\"\t11945\t8887\t23836\t2352\t0\t0\t0\t180\t146907\tFalse\tFalse\t0\t98997\t\"\"\t17\t18643\t\"Biota\"\tFalse\n" +
                "25\t1\t\"Eukaryota\"\t\"eukaryotes\"\tTrue\tFalse\t\"\"\t\"AAAA\"\t\"\"\t\"\"\tFalse\tFalse\t\"NATI\"\t41333\tFalse\t61787423\t0\t\"\"\t\"\"\t11847\t8606\t23827\t2352\t0\t0\t0\t23\t146905\tFalse\tFalse\t0\t98152\t\"\"\t18450\t\"Eukaryota\"\tFalse\n" +
                "72\t25\t\"Animalia\"\t\"animals\"\tTrue\tFalse\t\"\"\t\"AAAAAA\"\t\"\"\t\"\"\tFalse\tTrue\t\"NATI\"\t14970\tFals2552\t1\t2828\t0\t\"\"\t\"\"\t880\t3367\t9754\t4\t0\t0\t0\t37\t146906\tFalse\tFalse\t0\t19512\t\"\"\t14\t5991\t\"Animalia\"\tFalse\n" +
                "395\t157011\t\"Annelida\"\t\"segmented worms and leeches\"\tTrue\tFalse\t\"\"\t\"AAAAAAAAAAADABAHAA\"\t\"\"\t\"\"\tFalse\tFals\"NATI\"\t255\tFalse\t27\t0\t22\t0\t\"\"\t\"\"\t22\t34\t90\t0\t0\t0\t0\t10\t146911\tFalse\tFalse\t0\t90\t\"\"\t7\t25\t\"Annelida\"\tFalse\n" +
                "51\t157695\t\"Arthropoda\"\t\"arthropods\"\tFalse\tFalse\t\"\"\t\"AAAAAAAAAAADAAAA\"\t\"\"\t\"\"\tFalse\tTrue\t\"NATI\"\t1241False\t2004\t11\t2362\t0\t\"\"\t\"\"\t667\t2643\t8845\t4\t0\t0\t0\t9\t146911\tFalse\tFals16266\t\"\"\t3\t5392\t\"Arthropoda\"\tFalse\n" +
                "360\t51\t\"Arachnida\"\t\"mites, spiders, false scorpions, harvestmen etc.\"\tTrue\tFalse\t\"\"\t\"AAAAAAAAAAADAAAAAA\"\t\"\"\t\"\"\tFalse\tTrue\t\"NATI\"\t464\tFalse\t89\t0\t119\t0\t\"\"\t\"\"\t65\t127\t324\t0\t0\t0\t146912\tFalse\tFalse\t0\t457\t\"\"\t3\t190\t\"Arachnida\"\tFalse\n" +
                "358\t51\t\"Crustacea\"\t\"crustaceans\"\tTrue\tFalse\t\"\"\t\"AAAAAAAAAAADAAAAAB\"\t\"\"\t\"\"\tFalse\tFalse\t\"NATI\"\t587\tFalse\t84\t3\t79\t0\t\"\"\t\"\"\t154\t100\t295\t4\t0\t0\t0\t18\t146912\tFalse\tFals188\t\"\"\t13\t99\t\"Crustacea\"\tFalse\n" +
                "414\t358\t\"Branchiopoda\"\t\"branchiopods\"\tFalse\tFalse\t\"\"\t\"AAAAAAAAAAADAAAAABAA\"\t\"\"\t\"\"\tFalse\tFalse\t\"NATI\"\t30\tFalse\t3\t0\t11\t0\t\"\"\t\"\"\t88\t4\t19\t4\t0\t0\t0\t0\t146914\tFalse\tFals\"\"\t0\t3\t\"Branchiopoda\"\tFalse\n" +
                "417\t358\t\"Branchiura\"\t\"fish lice\"\tFalse\tFalse\t\"\"\t\"AAAAAAAAAAADAAAAABAB\"\t\"\"\t\"\"\tFalse\tFalse\t\"NATI\"\t0\tFalse\t1\t0\t1\t0\t\"\"\t\"\"\t0\t1\t0\t0\t0\t0\t0\t0\t146914\tFalse\tFals10\t\"\"\t0\t1\t\"Branchiura\"\tFalse";
        LabeledCSVParser labeledCSVParser = createParser(taxaString);
        return new StudyImporterForBioInfo(new ParserFactoryImpl(), nodeFactory).createTaxaMap(labeledCSVParser);
    }

    @Test(expected = StudyImporterException.class)
    public void relationsParsingMissingTaxon() throws IOException, NodeFactoryException, StudyImporterException {
        Map<Long, String> taxaMap = buildTaxaMap();
        Map<Long, RelType> relationsTypeMap = buildRelationsTypeMap();
        assertRelations(RELATIONS_STRING, taxaMap, relationsTypeMap);
    }

    @Test
    public void relationsParsing() throws IOException, NodeFactoryException, StudyImporterException {
        Map<Long, String> taxaMap = buildTaxaMap();
        taxaMap.put(43913L, "Homo sapiens");
        taxaMap.put(43912L, "some scientific name");
        taxaMap.put(3011L, "some scientific name");
        taxaMap.put(43917L, "some scientific name");
        taxaMap.put(43920L, "some scientific name");
        taxaMap.put(107544L, "some scientific name");
        taxaMap.put(1625L, "some scientific name");
        taxaMap.put(12958L, "some scientific name");
        taxaMap.put(43927L, "some scientific name");
        taxaMap.put(43930L, "some scientific name");
        taxaMap.put(32122L, "some scientific name");
        taxaMap.put(465L, "some scientific name");
        taxaMap.put(464L, "some scientific name");
        Map<Long, RelType> relationsTypeMap = buildRelationsTypeMap();
        assertRelations(RELATIONS_STRING, taxaMap, relationsTypeMap);
    }

    private void assertRelations(String relationsString, Map<Long, String> taxaMap, Map<Long, RelType> relationsTypeMap) throws IOException, StudyImporterException, NodeFactoryException {

        assertThat(nodeFactory.findTaxon("Homo sapiens"), is(nullValue()));

        LabeledCSVParser labeledCSVParser = createParser(relationsString);

        Study study = new StudyImporterForBioInfo(new ParserFactoryImpl(), nodeFactory).createRelations(taxaMap, relationsTypeMap, labeledCSVParser);

        Study study1 = nodeFactory.findStudy(study.getTitle());
        assertThat(study1, is(notNullValue()));
        Iterable<Relationship> specimens = study1.getSpecimens();
        List<Node> specimenList = new ArrayList<Node>();
        for (Relationship specimen : specimens) {
            assertThat(specimen.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING), is(notNullValue()));
            assertThat(specimen.getEndNode().getSingleRelationship(InteractType.INTERACTS_WITH, Direction.BOTH), is(notNullValue()));
            specimenList.add(specimen.getEndNode());
        }

        assertThat(specimenList.size(), is(18));
        assertThat(specimenList.get(0).getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING), is(notNullValue()));
        assertThat(specimenList.get(1).getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING), is(notNullValue()));

        assertThat(nodeFactory.findTaxon("Homo sapiens"), is(notNullValue()));
        assertThat(nodeFactory.findTaxon("some scientific name"), is(notNullValue()));
    }




    @Test
    public void trophicRelationsParser() throws IOException, StudyImporterException {
        Map<Long, RelType> relationsTypeMap = buildRelationsTypeMap();

        assertThat(relationsTypeMap.get(43899L), is((RelType)InteractType.PREYS_UPON));
        assertThat(relationsTypeMap.get(43900L), is((RelType)InteractType.PARASITE_OF));
        assertThat(relationsTypeMap.get(43901L), is((RelType)InteractType.HAS_HOST));
        assertThat(relationsTypeMap.get(43902L), is((RelType)InteractType.INTERACTS_WITH));
    }

    private Map<Long, RelType> buildRelationsTypeMap() throws IOException, StudyImporterException {
        String trophicRelations = "TrophicRel_id\tEnergyDonor\tEnergyRecipient\tTitle80\tNotes\tisFoodWeb\tPrimarySort8\tSecondarySort8\tisLiving\tisDead\tisMycorrhizal\t\n" +
                "43899\t\"is predated by\"\t\"is predator of\"\t\"Animal / predator\"\t\"Kills and feeds on this type of animal\"\tTrue\t\"A Anim\"\t\"A Pred\"\tTrue\tFalse\tFalse\n" +
                "43900\t\"is ectoparasitised by\"\t\"ectoparasitises\"\t\"Animal / parasite / ectoparasite\"\t\"derives its nutrition from a single living individual of another species with which it is closely associated but remains external to\"\tTrue\t\"A Anim\"\t\"EEC Par\"\tTrueFalse\tFalse\n" +
                "43901\t\"is ectomycorrhizal host of\"\t\"is ectomycorrhizal with\"\t\"Foodplant / mycorrhiza / ectomycorrhiza\"\t\"Exchanges simple inorganic chemicals from the soil for organic photosynthate from the host. <cr><cr>Very few of these relationships have been demonstrated experimentally. Most are deduced from field associations. In the future DNA-based analyses of ectomycorrhiza will increase the reliabilty of the data - and probably extend the range of host species.\"\tTrue\t\"P Plant\"\t\"EM Ect\"\tTrue\tFalse\tTrue\n" +
                "43902\t\"is galled by\"\t\"causes gall of\"\t\"Foodplant / gall\"\t\"Feeds within an abnormal growth caused by its presence on the host plant\"\tTrue\t\"P Plant\"\t\"R Gall\"\tTrue\tFalse\tFalse\n" +
                "43903\t\"with inquiline\"\t\"is inquiline in\"\t\"Animal / inquiline\"\t\"dwells in a gall caused by, or nest of, another species\"\tTrue\t\"A Anim\"\t\"E Inq\"\tTrue\tFalse\tFalse\n" +
                "45434\t\"is mined by\"\t\"mines\"\t\"Foodplant / miner\"\t\"Feeds under the surface (leaf, bark or stem miner) on the host plant\"\tTrue\t\"P Plant\"\t\"M1 Miner\"\tTrue\tFalse\tFalse\n" +
                "45436\t\"is endoparasitoid host of\"\t\"is endoparasitoid of\"\t\"Animal / parasitoid / endoparasitoid\"\t\"derives its nutrition from a single living individual of another species on which it is an endoparasite (internal parasite) and which finally dies as a result of these attentions\"\tTrue\t\"A Anim\"\t\"D PEN\"\tTrue\tFalse\tFalse\n" +
                "45437\t\"is grazed by\"\t\"grazes on\"\t\"Foodplant / open feeder\"\t\"Feeds in the open (ie not in a mine or case) on the surface of the plant\"\tTrue\t\"P Plant\"\t\"B Open\"\tTrue\tFalse\tFalse\n" +
                "134529\t\"is mutualistic with\"\t\"is mutualistic with\"\t\"Foodplant / mutualist\"\t\"A mutually beneficial exchange of nutrients between two individuals of different species.\"\tTrue\t\"P Plant\"\t\"E Symb\"\tTrue\tFalse\tFalse";
        LabeledCSVParser trophicRelationsParser = createParser(trophicRelations);
        return new StudyImporterForBioInfo(new ParserFactoryImpl(), nodeFactory).createRelationsTypeMap(trophicRelationsParser);
    }

    private LabeledCSVParser createParser(String trophicRelations) throws IOException {
        CSVParser parse = new CSVParser(new StringReader(trophicRelations));
        parse.changeDelimiter('\t');
        return new LabeledCSVParser(parse);
    }

}
