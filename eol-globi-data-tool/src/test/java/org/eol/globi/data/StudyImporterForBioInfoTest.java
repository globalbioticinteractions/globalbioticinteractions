package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForBioInfoTest extends GraphDBTestCase {

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
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporter importer = new StudyImporterFactory(new ParserFactoryImpl(), nodeFactory).instantiateImporter((Class) StudyImporterForBioInfo.class);
        // limit the number of line to be imported to make test runs reasonably fast
        importer.setFilter(new ImportFilter() {

            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 500
                        || recordNumber == 4585
                        || (recordNumber > 24220 && recordNumber < 24340);
            }
        });
        Study study = importer.importStudy();

        Iterable<Relationship> collectedRels = study.getSpecimens();
        for (Relationship collectedRel : collectedRels) {
            Specimen specimen = new Specimen(collectedRel.getEndNode());
            String externalId = specimen.getExternalId();
            assertThat(externalId, is(notNullValue()));
            assertThat(externalId, containsString(TaxonomyProvider.BIO_INFO + "rel:"));
        }

        ExecutionEngine engine = new ExecutionEngine(getGraphDb());
        ExecutionResult result = engine.execute("START taxon = node:taxons('*:*') MATCH taxon<-[:CLASSIFIED_AS]-specimen-[r]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon RETURN taxon.externalId + ' ' + lower(type(r)) + ' ' + targetTaxon.externalId");
        assertThat(result.dumpToString(), containsString("NBN:NHMSYS0000455771 interacts_with NBN:NBNSYS0000024890"));
        assertThat(result.dumpToString(), containsString("NBN:NBNSYS0000030148 parasite_of NBN:NHMSYS0000502366"));
        assertThat(result.dumpToString(), containsString("NBN:NHMSYS0000500943 has_parasite NBN:NBNSYS0000030148"));
        assertThat(result.dumpToString(), containsString("NBN:NHMSYS0000460576 eaten_by NBN:NHMSYS0020152444"));

        assertThat(study.getTitle(), is("BIO_INFO"));
    }



    @Test
    public void parseRelations() throws IOException, NodeFactoryException, StudyImporterException {
        assertRelations(RELATIONS_STRING);
    }

    private void assertRelations(String relationsString) throws IOException, StudyImporterException, NodeFactoryException {

        assertThat(nodeFactory.findTaxonByName("Homo sapiens"), is(nullValue()));

        LabeledCSVParser labeledCSVParser = createParser(relationsString);

        StudyImporterForBioInfo importer = new StudyImporterForBioInfo(new ParserFactoryImpl(), nodeFactory);
        Study study = importer.createStudy();
        importer.createRelations(labeledCSVParser, study);

        Study study1 = nodeFactory.findStudy(study.getTitle());
        assertThat(study1, is(notNullValue()));
        Iterable<Relationship> specimens = study1.getSpecimens();
        List<Node> specimenList = new ArrayList<Node>();
        for (Relationship specimen : specimens) {
            assertThat(specimen.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING), is(notNullValue()));
            assertThat(specimen.getEndNode().getSingleRelationship(InteractType.INTERACTS_WITH, Direction.OUTGOING), is(notNullValue()));
            assertThat(specimen.getEndNode().getSingleRelationship(InteractType.INTERACTS_WITH, Direction.INCOMING), is(notNullValue()));
            assertThat(specimen.getEndNode().getSingleRelationship(InteractType.INTERACTS_WITH, Direction.INCOMING), is(notNullValue()));
            specimenList.add(specimen.getEndNode());
        }

        assertThat(specimenList.size(), is(18));
        Relationship classifiedAs = specimenList.get(0).getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
        assertThat(classifiedAs, is(notNullValue()));
        assertThat((String)classifiedAs.getEndNode().getProperty(PropertyAndValueDictionary.EXTERNAL_ID), is("NBN:NBNSYS0000003949"));
        assertThat(specimenList.get(1).getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING), is(notNullValue()));

        assertThat(nodeFactory.findTaxonById(TaxonomyProvider.NBN.getIdPrefix() + "NBNSYS0000024889"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonById(TaxonomyProvider.NBN.getIdPrefix() + "NBNSYS0000024891"), is(notNullValue()));
    }


    private LabeledCSVParser createParser(String trophicRelations) throws IOException {
        CSVParser parse = new CSVParser(new StringReader(trophicRelations));
        return new LabeledCSVParser(parse);
    }

}
