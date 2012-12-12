package org.trophic.graph.data;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForAkinTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporter importer = new StudyImporterFactory(new ParserFactoryImpl(), nodeFactory).createImporterForStudy(StudyLibrary.Study.AKIN_MAD_ISLAND);
        importer.importStudy();

        assertNotNull(nodeFactory.findTaxonOfType("Sciaenops ocellatus"));
        assertNotNull(nodeFactory.findTaxonOfType("Paralichthys lethostigma"));
        assertNotNull(nodeFactory.findTaxonOfType("Adinia xenica"));
        assertNotNull(nodeFactory.findTaxonOfType("Citharichthys spilopterus"));
    }


    @Test
    public void importMappingIssue() throws StudyImporterException, NodeFactoryException {
        String csvString = ",\"Fish No\",\"Fish Species\",\"Date \",,\"Site \",\"SL(mm)\",\"Stomach volume\",\"Stomach status\",\"C. Method\",\"Detritus\",\"Sand\",\"Diatoms\",\"Centric diatoms\",\"Pennate diatoms\",\"Oscillatoria \",\"Filamentous algae\",\"Green algae\",\"Other blue green algae\",\"Unicellular green algae\",\"Brown algae\",\"Golden brown algae\",\"Nostocales (Blue green algae)\",\"Chara\",\"Ruppia maritima\",\"Other macrophytes\",\"Seeds\",\"Invertebrate eggs \",\"Fish bones\",\"Lucania parva eggs\",\"Other fish eggs\",\"Catfish egg\",\"Unidentified bone\",\"Fish eyes\",\"Fish otolith\",\"Ctenoid fish scale\",\"Cycloid fish scale\",\"Dinoflagellates (Noctiluca spp.)\",\"Protozoa\",\"Bivalvia\",\"Gastropoda\",\"Nematode\",\"Nemotode(Parasite)\",\"Calanoida copepoda\",\"Harpacticoida copepoda\",\"Copepoda nauplii\",\"Cyclopoid copepoda\",\"Cladocera\",\"Rotifera\",\"Unidefined zooplankton\",\"Chironomidae larvae\",\"Diptera 1\",\"Diptera 2\",\"Diptera midges(pupa)\",\"Polycheate worm\",\"Other annelid worms\",\"Amphipoda(Gammarus spp.)\",\"Corophium sp(amphipoda)\",\"Decopoda larvae\",\"Isopoda\",\"Unidentified invertebrate\",\"Other Ephemeroptera(Mayfly)\",\"Ephemeroptera(Baetidae)\",\"Coleoptera\",\"Other Hymenoptera\",\"Odonate\",\"Damselfly\",\"Other Thrips\",\"Thysanoptera (thrips)\",\"Pteromalitidae(Hymenoptera)\",\"Hemiptera\",\"Homoptera\",\"Unidentified insect larvae\",\"Arachnida \",\"Unidentified insects\",\"Mollusks (Oyster)\",\"Other Mollusks\",\"Ostracoda\",\"Brachyura (Crab zoea)\",\"Mysidacea\",\"Penneid shrimp post larvae\",\"Unidentified shrimp\",\"Palaemonotes pugio\",\"Peneaus setiferus\",\"Peneaus aztecus\",\"Callinectes sapidus\",\"Other crabs\",\"Neopunope sayi(mud crab)\",\"Myrophis punctatus (speckled worm eel)\",\"Mugil cephalus\",\"Brevoortia patronus\",\"Lepisosteus osseus \",\"Fundulus  grandis\",\"Other fundulus species\",\"Cyprinodon variegatus\",\"Pogonias cromis\",\"Menidia beryllina\",\"Anchoa mitchilli\",\"Other Sciaenidae species\",\"Lagodon rhomboides\",\"Arius felis\",\"Leiostomus xanthurus\",\"Gobiosoma bosc\",\"Lucania parva\",\"Micropogonias undulatus\",\"Cynoscion nebulosus\",\"Poecilia latipinna\",\"Unidentified fish\",\"Unidentified fish larvae\",\"Other Gobiidae \",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                ",9,\"Leiostomus xanthurus\",\"03.07.98\",,6,26.7,0,,\"Seine\",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\"EMPTY\",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";
        StudyImporter importer = new StudyImporterFactory(new TestParserFactory(csvString), nodeFactory).createImporterForStudy(StudyLibrary.Study.AKIN_MAD_ISLAND);
        importer.importStudy();
        Taxon taxon = nodeFactory.findTaxonOfType("Leiostomus xanthurus");
        assertNotNull(taxon);

    }

    @Test
    public void importHeaderAndASingleSpecimen() throws StudyImporterException, NodeFactoryException, ParseException {
        String csvString = ",\"Fish No\",\"Fish Species\",\"Date \",,\"Site \",\"SL(mm)\",\"Stomach volume\",\"Stomach status\",\"C. Method\",\"Detritus\",\"Sand\",\"Diatoms\",\"Centric diatoms\",\"Pennate diatoms\",\"Oscillatoria \",\"Filamentous algae\",\"Green algae\",\"Other blue green algae\",\"Unicellular green algae\",\"Brown algae\",\"Golden brown algae\",\"Nostocales (Blue green algae)\",\"Chara\",\"Ruppia maritima\",\"Other macrophytes\",\"Seeds\",\"Invertebrate eggs \",\"Fish bones\",\"Lucania parva eggs\",\"Other fish eggs\",\"Catfish egg\",\"Unidentified bone\",\"Fish eyes\",\"Fish otolith\",\"Ctenoid fish scale\",\"Cycloid fish scale\",\"Dinoflagellates (Noctiluca spp.)\",\"Protozoa\",\"Bivalvia\",\"Gastropoda\",\"Nematode\",\"Nemotode(Parasite)\",\"Calanoida copepoda\",\"Harpacticoida copepoda\",\"Copepoda nauplii\",\"Cyclopoid copepoda\",\"Cladocera\",\"Rotifera\",\"Unidefined zooplankton\",\"Chironomidae larvae\",\"Diptera 1\",\"Diptera 2\",\"Diptera midges(pupa)\",\"Polycheate worm\",\"Other annelid worms\",\"Amphipoda(Gammarus spp.)\",\"Corophium sp(amphipoda)\",\"Decopoda larvae\",\"Isopoda\",\"Unidentified invertebrate\",\"Other Ephemeroptera(Mayfly)\",\"Ephemeroptera(Baetidae)\",\"Coleoptera\",\"Other Hymenoptera\",\"Odonate\",\"Damselfly\",\"Other Thrips\",\"Thysanoptera (thrips)\",\"Pteromalitidae(Hymenoptera)\",\"Hemiptera\",\"Homoptera\",\"Unidentified insect larvae\",\"Arachnida \",\"Unidentified insects\",\"Mollusks (Oyster)\",\"Other Mollusks\",\"Ostracoda\",\"Brachyura (Crab zoea)\",\"Mysidacea\",\"Penneid shrimp post larvae\",\"Unidentified shrimp\",\"Palaemonotes pugio\",\"Peneaus setiferus\",\"Peneaus aztecus\",\"Callinectes sapidus\",\"Other crabs\",\"Neopunope sayi(mud crab)\",\"Myrophis punctatus (speckled worm eel)\",\"Mugil cephalus\",\"Brevoortia patronus\",\"Lepisosteus osseus \",\"Fundulus  grandis\",\"Other fundulus species\",\"Cyprinodon variegatus\",\"Pogonias cromis\",\"Menidia beryllina\",\"Anchoa mitchilli\",\"Other Sciaenidae species\",\"Lagodon rhomboides\",\"Arius felis\",\"Leiostomus xanthurus\",\"Gobiosoma bosc\",\"Lucania parva\",\"Micropogonias undulatus\",\"Cynoscion nebulosus\",\"Poecilia latipinna\",\"Unidentified fish\",\"Unidentified fish larvae\",\"Other Gobiidae \",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                ",1,\"Pogonias cromis\",\"03.07.98\",,1,226,3,,\"Gillnet\",,0.15,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0.6,,,,,,0.45,,,,,,,,,,,,,,,,,,1.35,0.45,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";
        StudyImporter importer = new StudyImporterFactory(new TestParserFactory(csvString), nodeFactory).createImporterForStudy(StudyLibrary.Study.AKIN_MAD_ISLAND);
        Study study = importer.importStudy();
        Taxon taxon = nodeFactory.findTaxonOfType("Pogonias cromis");
        assertNotNull(taxon);

        Iterable<Relationship> specimens = study.getSpecimens();
        Relationship rel = specimens.iterator().next();
        assertThat(rel, is(not(nullValue())));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date expectedDate = formatter.parse("1998-03-07");
        assertThat((Long)rel.getProperty(Specimen.DATE_IN_UNIX_EPOCH), is(expectedDate.getTime()));

        Node specimen = rel.getEndNode();
        assertThat((Double) specimen.getProperty(Specimen.LENGTH_IN_MM), is(226.0));
        assertThat((Double) specimen.getProperty(Specimen.STOMACH_VOLUME_ML), is(3.0));
        Node speciesNode = specimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode();
        assertThat((String) speciesNode.getProperty("name"), is("Pogonias cromis"));
        Iterable<Relationship> ateRels = specimen.getRelationships(RelTypes.ATE, Direction.OUTGOING);
        Map<String, Map<String, Object>> preys = new HashMap<String, Map<String, Object>>();
        for (Relationship ateRel : ateRels) {
            Node preyNode = ateRel.getEndNode();
            Node taxonNode = preyNode.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode();
            String name = (String) taxonNode.getProperty("name");
            HashMap<String, Object> propertyMap = new HashMap<String, Object>();
            propertyMap.put("name", name);
            propertyMap.put(Specimen.VOLUME_IN_ML, preyNode.getProperty(Specimen.VOLUME_IN_ML));
            preys.put(name, propertyMap);
        }
        Map<String, Object> sand = preys.get("Sand");
        assertThat(sand, is(notNullValue()));
        assertThat((String) sand.get("name"), is("Sand"));
        assertThat((Double) sand.get(Specimen.VOLUME_IN_ML), is(0.15d));
        Map<String, Object> chironomidae = preys.get("Chironomidae");
        assertThat(chironomidae, is(notNullValue()));
        assertThat((String) chironomidae.get("name"), is("Chironomidae"));
        assertThat((Double) chironomidae.get(Specimen.VOLUME_IN_ML), is(0.6d));

        Map<String, Object> amphipoda = preys.get("Amphipoda");
        assertThat(amphipoda, is(notNullValue()));
        assertThat((String) amphipoda.get("name"), is("Amphipoda"));
        assertThat((Double) amphipoda.get(Specimen.VOLUME_IN_ML), is(0.45d));

        Map<String, Object> insecta = preys.get("Insecta");
        assertThat(insecta, is(notNullValue()));
        assertThat((String) insecta.get("name"), is("Insecta"));
        assertThat((Double) insecta.get(Specimen.VOLUME_IN_ML), is(1.35d));

        Map<String, Object> mollusca = preys.get("Mollusca");
        assertThat(mollusca, is(notNullValue()));
        assertThat((String) mollusca.get("name"), is("Mollusca"));
        assertThat((Double) mollusca.get(Specimen.VOLUME_IN_ML), is(0.45d));

        Node locationNode = specimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING).getEndNode();
        assertThat((Double) locationNode.getProperty(Location.LATITUDE), is(28.645202d));
        assertThat((Double) locationNode.getProperty(Location.LONGITUDE), is(-96.099923d));
    }

}
