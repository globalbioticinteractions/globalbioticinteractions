package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.RelationshipListener;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DatasetImporterForBioInfoTest extends GraphDBTestCase {

    public static final String RELATIONS_STRING = "my relation id,relationship,active relation,passive relation,my active taxon id,active NBN Code,active url,is identity of active taxon uncertain,state of active taxon,part of active taxon,stage of active taxon,where stage of active taxon is recorded,importance of stage of active taxon,state of passive taxon,part of passive taxon,stage of passive taxon,where part of passive taxon is recorded,importance of part of passive taxon,season (numeric),season (alpha),indoors etc,relation recorded in GB/Ireland,importance of relationship,is nature of relationship uncertain,constructed sentence for active,constructed sentence for passive,my passive taxon id,passive NBN Code,passive url,is identity of passive taxon uncertain,list of reference ids\n" +
            "\"1534\",\"Plant / associate\",\"is associated with\",\"is associate of\",\"34737\",\"NBNSYS0000024889\",\"www.bioinfo.org.uk/html/Abdera_biflexuosa.htm\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"<active> is associated with <passive>\",\"<passive> is associate of <active>\",\"537\",\"NBNSYS0000003949\",\"www.bioinfo.org.uk/html/Fraxinus_excelsior.htm\",\"\",\"60527\"\n" +
            "\"2068\",\"Plant / associate\",\"is associated with\",\"is associate of\",\"34737\",\"NBNSYS0000024889\",\"www.bioinfo.org.uk/html/Abdera_biflexuosa.htm\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"<active> is associated with <passive>\",\"<passive> is associate of <active>\",\"3078\",\"NHMSYS0000462211\",\"www.bioinfo.org.uk/html/Quercus.htm\",\"\",\"60527\"\n" +
            "\"1139\",\"Plant / associate\",\"is associated with\",\"is associate of\",\"34738\",\"NBNSYS0000024890\",\"www.bioinfo.org.uk/html/Abdera_flexuosa.htm\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"<active> is associated with <passive>\",\"<passive> is associate of <active>\",\"473\",\"NHMSYS0000455771\",\"www.bioinfo.org.uk/html/Alnus_glutinosa.htm\",\"\",\"60527\"\n" +
            "\"2210\",\"Plant / associate\",\"is associated with\",\"is associate of\",\"34738\",\"NBNSYS0000024890\",\"www.bioinfo.org.uk/html/Abdera_flexuosa.htm\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"<active> is associated with <passive>\",\"<passive> is associate of <active>\",\"6240\",\"NHMSYS0000463078\",\"www.bioinfo.org.uk/html/Salix.htm\",\"\",\"60527\"\n" +
            "\"1321\",\"Plant / associate\",\"is associated with\",\"is associate of\",\"34739\",\"NBNSYS0000024891\",\"www.bioinfo.org.uk/html/Abdera_quadrifasciata.htm\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"<active> is associated with <passive>\",\"<passive> is associate of <active>\",\"1186\",\"NBNSYS0000003838\",\"www.bioinfo.org.uk/html/Carpinus_betulus.htm\",\"\",\"60527\"\n" +
            "\"1502\",\"Plant / associate\",\"is associated with\",\"is associate of\",\"34739\",\"NBNSYS0000024891\",\"www.bioinfo.org.uk/html/Abdera_quadrifasciata.htm\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"<active> is associated with <passive>\",\"<passive> is associate of <active>\",\"885\",\"NBNSYS0000003840\",\"www.bioinfo.org.uk/html/Fagus_sylvatica.htm\",\"\",\"60527\"\n" +
            "\"2069\",\"Plant / associate\",\"is associated with\",\"is associate of\",\"34739\",\"NBNSYS0000024891\",\"www.bioinfo.org.uk/html/Abdera_quadrifasciata.htm\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"<active> is associated with <passive>\",\"<passive> is associate of <active>\",\"3078\",\"NHMSYS0000462211\",\"www.bioinfo.org.uk/html/Quercus.htm\",\"\",\"60527\"\n" +
            "\"1870\",\"Plant / associate\",\"is associated with\",\"is associate of\",\"34740\",\"NBNSYS0000024892\",\"www.bioinfo.org.uk/html/Abdera_triguttata.htm\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"<active> is associated with <passive>\",\"<passive> is associate of <active>\",\"42202\",\"NHMSYS0000461702\",\"www.bioinfo.org.uk/html/Pinus.htm\",\"\",\"60527\"\n" +
            "\"4029\",\"Foodplant / open feeder\",\"grazes on\",\"is grazed by\",\"102829\",\"NHMSYS0020480647\",\"www.bioinfo.org.uk/html/Abia_sericea.htm\",\"\",\"\",\"\",\"larva\",\"\",\"\",\"\",\"leaf\",\"\",\"\",\"\",\" \",\"\",\"\",\"\",\"\",\"\",\"larva of <active> grazes on leaf of <passive>\",\"leaf of <passive> is grazed by larva of <active>\",\"1283\",\"NBNSYS0000004352\",\"www.bioinfo.org.uk/html/Succisa_pratensis.htm\",\"\",\"60536\"\n";

    @Test
    public void importAbout600Records() throws StudyImporterException {
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory, getClass())
                .instantiateImporter(DatasetImporterForBioInfo.class);
        final List<String> msgs = new ArrayList<String>();
        importer.setLogger(new ImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                msgs.add(message);
            }

            @Override
            public void info(LogContext ctx, String message) {
                msgs.add(message);
            }

            @Override
            public void severe(LogContext ctx, String message) {
                msgs.add(message);
            }
        });
        // limit the number of line to be imported to make test runs reasonably fast
        importer.setFilter(recordNumber -> recordNumber < 1000
                || recordNumber == 4585
                || (recordNumber > 47310 && recordNumber < 47320)
                || (recordNumber > 24220 && recordNumber < 24340));
        importStudy(importer);

        StudyImpl study1 = new StudyImpl(TaxonomyProvider.BIO_INFO + "ref:153303");
        study1.setExternalId("http://bioinfo.org.uk/html/b153303.htm");
        Study vectorStudy = nodeFactory.findStudy(study1);
        assertThat(vectorStudy, is(notNullValue()));

        StudyImpl study2 = new StudyImpl(TaxonomyProvider.BIO_INFO + "ref:60527");
        study2.setExternalId("http://bioinfo.org.uk/html/b60527.htm");
        StudyNode study = (StudyNode) nodeFactory.findStudy(study2);

        AtomicBoolean success = new AtomicBoolean(false);
        NodeUtil.handleCollectedRelationships(
                new NodeTypeDirection(study.getUnderlyingNode()),
                relationship -> {
                    SpecimenNode specimen = new SpecimenNode(relationship.getEndNode());
                    String externalId = specimen.getExternalId();
                    assertThat(externalId, is(notNullValue()));
                    assertThat(externalId, CoreMatchers.containsString(TaxonomyProvider.BIO_INFO + "rel:"));
                    success.set(true);
                });

        assertTrue(success.get());

        Result result = getGraphDb().execute("CYPHER 2.3 START taxon = node:taxons('*:*') MATCH taxon<-[:CLASSIFIED_AS]-specimen-[r]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon RETURN taxon.externalId + ' ' + lower(type(r)) + ' ' + targetTaxon.externalId as interaction");
        List<String> interactions = new ArrayList<String>();
        while (((ResourceIterator<Map<String, Object>>) result).hasNext()) {
            Map<String, Object> next = ((ResourceIterator<Map<String, Object>>) result).next();
            interactions.add((String) next.get("interaction"));
        }
        assertThat(interactions, CoreMatchers.hasItem("NBN:NHMSYS0000455771 interacts_with NBN:NBNSYS0000024890"));
        assertThat(interactions, CoreMatchers.hasItem("NBN:NBNSYS0000030148 endoparasitoid_of NBN:NHMSYS0000502366"));
        assertThat(interactions, CoreMatchers.hasItem("NBN:NHMSYS0000500943 has_endoparasitoid NBN:NBNSYS0000030148"));
        assertThat(interactions, CoreMatchers.hasItem("bioinfo:taxon:160260 has_vector bioinfo:taxon:162065"));

        assertThat(study.getTitle(), is("bioinfo:ref:60527"));

        assertThat("found unexpected log messages: [" + StringUtils.join(msgs, "\n") + "]", msgs.size(), is(1));
        assertThat(msgs.get(0), is("empty/no taxon name for bioinfo taxon id [149359] on line [4171]"));
    }


    @Test
    public void parseSomeRelations() throws IOException, StudyImporterException {

        assertThat(taxonIndex.findTaxonByName("Homo sapiens"), is(nullValue()));

        LabeledCSVParser labeledCSVParser = createParser(RELATIONS_STRING);

        DatasetImporterForBioInfo importer = new DatasetImporterForBioInfo(
                new ParserFactoryLocal(getClass()), nodeFactory
        );

        importer.createRelations(labeledCSVParser, new TreeMap<String, String>() {{
            put("60527", "citation A");
            put("60536", "citation B");
        }}, new TreeMap<>());
        resolveNames();

        StudyImpl study2 = new StudyImpl(TaxonomyProvider.BIO_INFO + "ref:60536");
        study2.setExternalId("http://bioinfo.org.uk/html/b60536.htm");
        Study study = nodeFactory.findStudy(study2);
        assertNotNull(study);
        assertThat(study.getExternalId(), is("http://bioinfo.org.uk/html/b60536.htm"));
        assertNull(nodeFactory.findStudy(new StudyImpl(TaxonomyProvider.BIO_INFO + "ref:bla")));
        StudyImpl study3 = new StudyImpl(TaxonomyProvider.BIO_INFO + "ref:60527");
        study3.setExternalId("http://bioinfo.org.uk/html/b60527.htm");
        StudyNode study1 = (StudyNode) nodeFactory.findStudy(study3);
        assertThat(study1.getCitation(), is("citation A"));
        assertThat(study1, is(notNullValue()));
        List<Node> specimenList = new ArrayList<Node>();
        RelationshipListener handler = relationship -> {
            assertThat(relationship.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING), is(notNullValue()));
            assertThat(relationship.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(InteractType.INTERACTS_WITH), Direction.OUTGOING), is(notNullValue()));
            assertThat(relationship.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(InteractType.INTERACTS_WITH), Direction.INCOMING), is(notNullValue()));
            assertThat(relationship.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(InteractType.INTERACTS_WITH), Direction.INCOMING), is(notNullValue()));
            specimenList.add(relationship.getEndNode());
        };

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study1.getUnderlyingNode()), handler);


        assertThat(specimenList.size(), is(16));
        Relationship classifiedAs = specimenList.get(0).getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
        assertThat(classifiedAs, is(notNullValue()));
        assertThat((String) classifiedAs.getEndNode().getProperty(PropertyAndValueDictionary.EXTERNAL_ID), startsWith("NBN:NBNSYS"));
        assertThat(specimenList.get(1).getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING), is(notNullValue()));
        assertThat(taxonIndex.findTaxonById(TaxonomyProvider.NBN.getIdPrefix() + "NBNSYS0000024889"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonById(TaxonomyProvider.NBN.getIdPrefix() + "NBNSYS0000024891"), is(notNullValue()));
    }

    private LabeledCSVParser createParser(String csvString) throws IOException {
        return CSVTSVUtil.createLabeledCSVParser(new StringReader(csvString));
    }

    @Test
    public void importReferences() throws IOException {
        String firstFewlines = "BioInfo reference id,BioInfo url,author,year,title,reference type,edition,BioInfo reference id of the source (journal/book/publisher etc),source author,source title,source journal short title,source year,source reference type,source ISSN/ISBN,volume,series,page range,no of pages,ISSN/ISBN,URL of online source\n" +
                "\"149326\",\"www.bioinfo.org.uk/html/b149326.htm\",\"\",\"\",\"Agrobacterium tumefaciens\",\"Web Site/Page\",\"\",\"0\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"http://en.wikipedia.org/Agrobacterium_tumefaciens\"\n" +
                "\"147341\",\"www.bioinfo.org.uk/html/b147341.htm\",\"\",\"\",\"www.seabean.com\",\"Web Site/Page\",\"\",\"0\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"http://www.seabean.com\"\n" +
                "\"148459\",\"www.bioinfo.org.uk/html/b148459.htm\",\"\",\"\",\"British Leafminers\",\"Web Site/Page\",\"\",\"0\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"http://www.leafmines.co.uk/\"\n" +
                "\"148671\",\"www.bioinfo.org.uk/html/b148671.htm\",\"\",\"\",\"Sawflies discussion group\",\"E-forum\",\"\",\"148672\",\"\",\"Yahoo\",\"\",\"\",\"Publisher\",\"\",\"\",\"\",\"\",\"\",\"\",\"http://tech.groups.yahoo.com/group/sawfly/join\"\n" +
                "\"149380\",\"www.bioinfo.org.uk/html/b149380.htm\",\"\",\"\",\"Cuttlefish\",\"Web Site/Page\",\"\",\"0\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"http://www.pznow.co.uk/marine/cuttlefish.html\"\n" +
                "\"149878\",\"www.bioinfo.org.uk/html/b149878.htm\",\"\",\"\",\"The Marine Life Information Network for Britain and Ireland (MarLIN)\",\"Web Site/Page\",\"\",\"0\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"http://www.marlin.ac.uk\"\n" +
                "\"150118\",\"www.bioinfo.org.uk/html/b150118.htm\",\"\",\"2008\",\"Bacterial bleeding canker of horse chestnut\",\"Paper\",\"\",\"150094\",\"FERA\",\"Plant Clinic News\",\"\",\"\",\"Journal\",\"\",\"May 08\",\"\",\"2\",\"1\",\"\",\"\"\n" +
                "\"150071\",\"www.bioinfo.org.uk/html/b150071.htm\",\"\",\"\",\"Pyrenopeziza brassicae - CropMonitor\",\"Web Site/Page\",\"\",\"0\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"http://www.cropmonitor.co.uk/wosr/encyclopaedia/view_icard.cfm?cslref=12680\"\n" +
                "\"60527\",\"www.bioinfo.org.uk/html/b60527.htm\",\"Bullock, J.A.\",\"1992\",\"Host Plants of British Beetles: A List of Recorded Associations\",\"Book/Report\",\"\",\"147501\",\"\",\"Amateur Entomologists' Society\",\"AES\",\"\",\"Publisher\",\"\",\"11a\",\"\",\"\",\"24\",\"0 900054 56 5\",\"\"\n" +
                "\"150095\",\"www.bioinfo.org.uk/html/b150095.htm\",\"\",\"2009\",\"Verbena downy mildew\",\"Paper\",\"\",\"150094\",\"FERA\",\"Plant Clinic News\",\"\",\"\",\"Journal\",\"\",\"Sept 09\",\"\",\"1\",\"\",\"\",\"\"\n";
        final LabeledCSVParser parser = createParser(firstFewlines);
        Map<String, String> refIdMap = DatasetImporterForBioInfo.buildRefMap(parser);
        assertThat(refIdMap.get("149326"), is("Agrobacterium tumefaciens. Accessed at: http://en.wikipedia.org/Agrobacterium_tumefaciens"));
        assertThat(refIdMap.get("149878"), is("The Marine Life Information Network for Britain and Ireland (MarLIN). Accessed at: http://www.marlin.ac.uk"));
        assertThat(refIdMap.get("150118"), is("Bacterial bleeding canker of horse chestnut. Plant Clinic News. 2008. Vol May 08. pp 2"));
        assertThat(refIdMap.get("150095"), is("Verbena downy mildew. Plant Clinic News. 2009. Vol Sept 09. pp 1"));
        assertThat(refIdMap.get("60527"), is("Bullock, J.A.. 1992. Host Plants of British Beetles: A List of Recorded Associations. Amateur Entomologists' Society. Vol 11a"));
    }

    @Test
    public void importTaxa() throws IOException {
        String firstFewlines = "my taxon id,rank,latin,authority,english,NBN Code,family,order,phylum,url\n" +
                "\"268\",\"Informal\",\"'Chenopodiaceae'\",\"\",\"the old Chenopodiaceae\",\"\",\"Amaranthaceae\",\"Caryophyllales\",\"Tracheophyta\",\"www.bioinfo.org.uk/html/t268.htm\"\n" +
                "\"162827\",\"Species\",\"Abacarus hystrix\",\"(Nalepa, 1896)\",\"a mite\",\"NHMSYS0020190380\",\"Eriophyidae\",\"Trombidiformes\",\"Arthropoda\",\"www.bioinfo.org.uk/html/t162827.htm\"\n" +
                "\"41886\",\"Genus\",\"Abdera\",\"Stephens, 1832\",\"a genus of false darkling beetles\",\"NHMSYS0020151134\",\"Melandryidae\",\"Coleoptera\",\"Arthropoda\",\"www.bioinfo.org.uk/html/t41886.htm\"\n" +
                "\"34737\",\"Species\",\"Abdera biflexuosa\",\"(Curtis, 1829)\",\"a false darkling beetle\",\"NBNSYS0000024889\",\"Melandryidae\",\"Coleoptera\",\"Arthropoda\",\"www.bioinfo.org.uk/html/t34737.htm\"\n" +
                "\"34738\",\"Species\",\"Abdera flexuosa\",\"(Paykull, 1799)\",\"a false darkling beetle\",\"NBNSYS0000024890\",\"Melandryidae\",\"Coleoptera\",\"Arthropoda\",\"www.bioinfo.org.uk/html/t34738.htm\"\n" +
                "\"34739\",\"Species\",\"Abdera quadrifasciata\",\"(Curtis, 1829)\",\"a false darkling beetle\",\"NBNSYS0000024891\",\"Melandryidae\",\"Coleoptera\",\"Arthropoda\",\"www.bioinfo.org.uk/html/t34739.htm\"\n" +
                "\"34740\",\"Species\",\"Abdera triguttata\",\"(Gyllenhal, 1810)\",\"a false darkling beetle\",\"NBNSYS0000024892\",\"Melandryidae\",\"Coleoptera\",\"Arthropoda\",\"www.bioinfo.org.uk/html/t34740.htm\"\n" +
                "\"102829\",\"Species\",\"Abia sericea\",\"(Linnaeus, 1767)\",\"a clubhorned sawfly\",\"NHMSYS0020480647\",\"Cimbicidae\",\"Hymenoptera\",\"Arthropoda\",\"www.bioinfo.org.uk/html/t102829.htm\"\n" +
                "\"43913\",\"Genus\",\"Abies\",\"Mill.\",\"firs\",\"NHMSYS0000455511\",\"Pinaceae\",\"Pinales\",\"Tracheophyta\",\"www.bioinfo.org.uk/html/t43913.htm\"\n";
        final LabeledCSVParser parser = createParser(firstFewlines);
        Map<String, Taxon> taxonMap = DatasetImporterForBioInfo.buildTaxonMap(parser);
        assertThat(taxonMap.get("268").getName(), is("Chenopodiaceae"));
        assertThat(taxonMap.get("41886"), is(nullValue()));
    }

}
