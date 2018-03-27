package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class StudyImporterForAkinTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(StudyImporterForAkin.class);
        importStudy(importer);

        assertNotNull(taxonIndex.findTaxonByName("Sciaenops ocellatus"));
        assertNotNull(taxonIndex.findTaxonByName("Paralichthys lethostigma"));
        assertNotNull(taxonIndex.findTaxonByName("Adinia xenica"));
        assertNotNull(taxonIndex.findTaxonByName("Citharichthys spilopterus"));
    }

    @Test
    public void parseLifeStage() throws TermLookupServiceException {
        UberonLookupService service = new UberonLookupService();
        assertThat(StudyImporterForAkin.parseLifeStage(service, "something egg").get(0).getId(), is("UBERON:0007379"));
        assertThat(StudyImporterForAkin.parseLifeStage(service, "something eggs").get(0).getId(), is("UBERON:0007379"));
        assertThat(StudyImporterForAkin.parseLifeStage(service, "something larvae").get(0).getId(), is("UBERON:0000069"));
        assertThat(StudyImporterForAkin.parseLifeStage(service, "something zoea").get(0).getId(), is("UBERON:0000069"));
    }

    @Test
    public void importMappingIssue() throws StudyImporterException {
        String csvString = ",\"Fish No\",\"Fish Species\",\"Date \",,\"Site \",\"SL(mm)\",\"Stomach volume\",\"Stomach status\",\"C. Method\",\"Detritus\",\"Sand\",\"Diatoms\",\"Centric diatoms\",\"Pennate diatoms\",\"Oscillatoria \",\"Filamentous algae\",\"Green algae\",\"Other blue green algae\",\"Unicellular green algae\",\"Brown algae\",\"Golden brown algae\",\"Nostocales (Blue green algae)\",\"Chara\",\"Ruppia maritima\",\"Other macrophytes\",\"Seeds\",\"Invertebrate eggs \",\"Fish bones\",\"Lucania parva eggs\",\"Other fish eggs\",\"Catfish egg\",\"Unidentified bone\",\"Fish eyes\",\"Fish otolith\",\"Ctenoid fish scale\",\"Cycloid fish scale\",\"Dinoflagellates (Noctiluca spp.)\",\"Protozoa\",\"Bivalvia\",\"Gastropoda\",\"Nematode\",\"Nemotode(Parasite)\",\"Calanoida copepoda\",\"Harpacticoida copepoda\",\"Copepoda nauplii\",\"Cyclopoid copepoda\",\"Cladocera\",\"Rotifera\",\"Unidefined zooplankton\",\"Chironomidae larvae\",\"Diptera 1\",\"Diptera 2\",\"Diptera midges(pupa)\",\"Polycheate worm\",\"Other annelid worms\",\"Amphipoda(Gammarus spp.)\",\"Corophium sp(amphipoda)\",\"Decopoda larvae\",\"Isopoda\",\"Unidentified invertebrate\",\"Other Ephemeroptera(Mayfly)\",\"Ephemeroptera(Baetidae)\",\"Coleoptera\",\"Other Hymenoptera\",\"Odonate\",\"Damselfly\",\"Other Thrips\",\"Thysanoptera (thrips)\",\"Pteromalitidae(Hymenoptera)\",\"Hemiptera\",\"Homoptera\",\"Unidentified insect larvae\",\"Arachnida \",\"Unidentified insects\",\"Mollusks (Oyster)\",\"Other Mollusks\",\"Ostracoda\",\"Brachyura (Crab zoea)\",\"Mysidacea\",\"Penneid shrimp post larvae\",\"Unidentified shrimp\",\"Palaemonotes pugio\",\"Peneaus setiferus\",\"Peneaus aztecus\",\"Callinectes sapidus\",\"Other crabs\",\"Neopunope sayi(mud crab)\",\"Myrophis punctatus (speckled worm eel)\",\"Mugil cephalus\",\"Brevoortia patronus\",\"Lepisosteus osseus \",\"Fundulus  grandis\",\"Other fundulus species\",\"Cyprinodon variegatus\",\"Pogonias cromis\",\"Menidia beryllina\",\"Anchoa mitchilli\",\"Other Sciaenidae species\",\"Lagodon rhomboides\",\"Arius felis\",\"Leiostomus xanthurus\",\"Gobiosoma bosc\",\"Lucania parva\",\"Micropogonias undulatus\",\"Cynoscion nebulosus\",\"Poecilia latipinna\",\"Unidentified fish\",\"Unidentified fish larvae\",\"Other Gobiidae \",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                ",9,\"Leiostomus xanthurus\",\"03.07.98\",,6,26.7,0,,\"Seine\",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\"EMPTY\",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";
        StudyImporter importer = new StudyImporterTestFactory(new TestParserFactory(csvString), nodeFactory).instantiateImporter((Class) StudyImporterForAkin.class);
        importStudy(importer);
        Taxon taxon = taxonIndex.findTaxonByName("Leiostomus xanthurus");
        assertNotNull(taxon);

    }

    @Test
    public void importHeaderAndASingleSpecimen() throws StudyImporterException, ParseException {
        String csvString = ",\"Fish No\",\"Fish Species\",\"Date \",,\"Site \",\"SL(mm)\",\"Stomach volume\",\"Stomach status\",\"C. Method\",\"Detritus\",\"Sand\",\"Diatoms\",\"Centric diatoms\",\"Pennate diatoms\",\"Oscillatoria \",\"Filamentous algae\",\"Green algae\",\"Other blue green algae\",\"Unicellular green algae\",\"Brown algae\",\"Golden brown algae\",\"Nostocales (Blue green algae)\",\"Chara\",\"Ruppia maritima\",\"Other macrophytes\",\"Seeds\",\"Invertebrate eggs \",\"Fish bones\",\"Lucania parva eggs\",\"Other fish eggs\",\"Catfish egg\",\"Unidentified bone\",\"Fish eyes\",\"Fish otolith\",\"Ctenoid fish scale\",\"Cycloid fish scale\",\"Dinoflagellates (Noctiluca spp.)\",\"Protozoa\",\"Bivalvia\",\"Gastropoda\",\"Nematode\",\"Nemotode(Parasite)\",\"Calanoida copepoda\",\"Harpacticoida copepoda\",\"Copepoda nauplii\",\"Cyclopoid copepoda\",\"Cladocera\",\"Rotifera\",\"Unidefined zooplankton\",\"Chironomidae larvae\",\"Diptera 1\",\"Diptera 2\",\"Diptera midges(pupa)\",\"Polycheate worm\",\"Other annelid worms\",\"Amphipoda(Gammarus spp.)\",\"Corophium sp(amphipoda)\",\"Decopoda larvae\",\"Isopoda\",\"Unidentified invertebrate\",\"Other Ephemeroptera(Mayfly)\",\"Ephemeroptera(Baetidae)\",\"Coleoptera\",\"Other Hymenoptera\",\"Odonate\",\"Damselfly\",\"Other Thrips\",\"Thysanoptera (thrips)\",\"Pteromalitidae(Hymenoptera)\",\"Hemiptera\",\"Homoptera\",\"Unidentified insect larvae\",\"Arachnida \",\"Unidentified insects\",\"Mollusks (Oyster)\",\"Other Mollusks\",\"Ostracoda\",\"Brachyura (Crab zoea)\",\"Mysidacea\",\"Penneid shrimp post larvae\",\"Unidentified shrimp\",\"Palaemonotes pugio\",\"Peneaus setiferus\",\"Peneaus aztecus\",\"Callinectes sapidus\",\"Other crabs\",\"Neopunope sayi(mud crab)\",\"Myrophis punctatus (speckled worm eel)\",\"Mugil cephalus\",\"Brevoortia patronus\",\"Lepisosteus osseus \",\"Fundulus  grandis\",\"Other fundulus species\",\"Cyprinodon variegatus\",\"Pogonias cromis\",\"Menidia beryllina\",\"Anchoa mitchilli\",\"Other Sciaenidae species\",\"Lagodon rhomboides\",\"Arius felis\",\"Leiostomus xanthurus\",\"Gobiosoma bosc\",\"Lucania parva\",\"Micropogonias undulatus\",\"Cynoscion nebulosus\",\"Poecilia latipinna\",\"Unidentified fish\",\"Unidentified fish larvae\",\"Other Gobiidae \",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                ",1,\"Pogonias cromis\",\"03.07.98\",,1,226,3,,\"Gillnet\",,0.15,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0.6,,,,,,0.45,,,,,,,,,,,,,,,,,,1.35,0.45,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";
        StudyImporter importer = new StudyImporterTestFactory(new TestParserFactory(csvString), nodeFactory).instantiateImporter((Class) StudyImporterForAkin.class);
        importStudy(importer);

        Study study = getStudySingleton(getGraphDb());

        Taxon taxon = taxonIndex.findTaxonByName("Pogonias cromis");
        assertNotNull(taxon);

        Iterable<Relationship> specimens = NodeUtil.getSpecimens(study);
        Relationship rel = specimens.iterator().next();
        assertThat(rel, is(not(nullValue())));

        Date expectedDate = DateUtil.parsePatternUTC("1998-03-07", "yyyy-MM-dd").toDate();
        assertThat(rel.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH), is(expectedDate.getTime()));

        Node specimenNode = rel.getEndNode();
        assertThat(specimenNode.getProperty(SpecimenConstant.LENGTH_IN_MM), is(226.0));
        assertThat(specimenNode.getProperty(SpecimenConstant.STOMACH_VOLUME_ML), is(3.0));

        Specimen specimen = new SpecimenNode(specimenNode);
        assertThat(specimen.getSampleLocation().getAltitude(), is(-0.7));

        Node speciesNode = specimenNode.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).getEndNode();
        assertThat((String) speciesNode.getProperty("name"), is("Pogonias cromis"));
        Iterable<Relationship> ateRels = specimenNode.getRelationships(NodeUtil.asNeo4j(InteractType.ATE), Direction.OUTGOING);
        Map<String, Map<String, Object>> preys = new HashMap<String, Map<String, Object>>();
        for (Relationship ateRel : ateRels) {
            Node preyNode = ateRel.getEndNode();
            Node taxonNode = preyNode.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).getEndNode();
            String name = (String) taxonNode.getProperty("name");
            HashMap<String, Object> propertyMap = new HashMap<String, Object>();
            propertyMap.put("name", name);
            propertyMap.put(SpecimenConstant.VOLUME_IN_ML, preyNode.getProperty(SpecimenConstant.VOLUME_IN_ML));
            preys.put(name, propertyMap);
        }
        Map<String, Object> sand = preys.get("Sand");
        assertThat(sand, is(notNullValue()));
        assertThat((String) sand.get("name"), is("Sand"));
        assertThat((Double) sand.get(SpecimenConstant.VOLUME_IN_ML), is(0.15d));
        Map<String, Object> chironomidae = preys.get("Chironomidae larvae");
        assertThat(chironomidae, is(notNullValue()));
        assertThat((String) chironomidae.get("name"), is("Chironomidae larvae"));
        assertThat((Double) chironomidae.get(SpecimenConstant.VOLUME_IN_ML), is(0.6d));

        Map<String, Object> amphipoda = preys.get("Amphipoda(Gammarus spp.)");
        assertThat(amphipoda, is(notNullValue()));
        assertThat((String) amphipoda.get("name"), is("Amphipoda(Gammarus spp.)"));
        assertThat((Double) amphipoda.get(SpecimenConstant.VOLUME_IN_ML), is(0.45d));

        Map<String, Object> insecta = preys.get("Unidentified insects");
        assertThat(insecta, is(notNullValue()));
        assertThat((String) insecta.get("name"), is("Unidentified insects"));
        assertThat((Double) insecta.get(SpecimenConstant.VOLUME_IN_ML), is(1.35d));

        Map<String, Object> mollusca = preys.get("Mollusks (Oyster)");
        assertThat(mollusca, is(notNullValue()));
        assertThat((String) mollusca.get("name"), is("Mollusks (Oyster)"));
        assertThat((Double) mollusca.get(SpecimenConstant.VOLUME_IN_ML), is(0.45d));

        Node locationNode = specimenNode.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.OUTGOING).getEndNode();
        assertThat((Double) locationNode.getProperty(LocationConstant.LATITUDE), is(28.645202d));
        assertThat((Double) locationNode.getProperty(LocationConstant.LONGITUDE), is(-96.099923d));
    }

}
