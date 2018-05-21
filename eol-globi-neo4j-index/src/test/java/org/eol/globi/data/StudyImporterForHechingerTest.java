package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.Study;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForHechingerTest extends GraphDBTestCase {

    private static final Log LOG = LogFactory.getLog(StudyImporterForHechingerTest.class);

    @Test
    public void importStudy() throws StudyImporterException, IOException {
        JsonNode config = new ObjectMapper().readTree("{ \"citation\": \"Ryan F. Hechinger, Kevin D. Lafferty, John P. McLaughlin, Brian L. Fredensborg, Todd C. Huspeni, Julio Lorda, Parwant K. Sandhu, Jenny C. Shaw, Mark E. Torchin, Kathleen L. Whitney, and Armand M. Kuris 2011. Food webs including parasites, biomass, body sizes, and life stages for three California/Baja California estuaries. Ecology 92:791–791. https://doi.org/10.1890/10-1383.1 .\",\n" +
                "  \"doi\": \"https://doi.org/10.1890/10-1383.1\",\n" +
                "  \"format\": \"hechinger\",\n" +
                "  \"delimiter\": \"\\t\",\n" +
                "  \"resources\": {\n" +
                "    \"nodes\": \"hechinger/Metaweb_Nodes.txt\",\n" +
                "    \"links\": \"hechinger/Metaweb_Links.txt\"\n" +
                "  }\n" +
                "}");
        DatasetImpl dataset = new DatasetLocal();
        dataset.setConfig(config);
        ParserFactory parserFactory = new ParserFactoryForDataset(dataset);
        StudyImporterForHechinger importer = new StudyImporterForHechinger(parserFactory, nodeFactory);
        importer.setDataset(dataset);

        importer.setLogger(new ImportLogger() {
            @Override
            public void warn(LogContext study, String message) {
                LOG.warn(message);
            }

            @Override
            public void info(LogContext study, String message) {
                LOG.info(message);
            }

            @Override
            public void severe(LogContext study, String message) {
                LOG.error(message);
            }
        });
        importStudy(importer);

        Study study = getStudySingleton(getGraphDb());

        assertThat(study, is(notNullValue()));

        Iterable<Relationship> specimens = NodeUtil.getSpecimens(study);
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }
        assertThat(count, is(27932));


        ExecutionEngine engine = new ExecutionEngine(getGraphDb());
        String query = "START resourceTaxon = node:taxons(name='Suaeda spp.')" +
                " MATCH taxon<-[:CLASSIFIED_AS]-specimen-[r]->resourceSpecimen-[:CLASSIFIED_AS]-resourceTaxon, specimen-[:COLLECTED_AT]->location" +
                " RETURN taxon.name, specimen.lifeStage?, type(r), resourceTaxon.name, resourceSpecimen.lifeStage?, location.latitude as lat, location.longitude as lng";
        ExecutionResult result = engine.execute(query);

        assertThat(result.dumpToString(), containsString("Branta bernicla"));
        assertThat(result.dumpToString(), containsString("Athya affinis"));
        assertThat(result.dumpToString(), containsString("Anas acuta"));
        assertThat(result.dumpToString(), containsString("30.378207 | -115.938835 |"));

        query = "START taxon = node:taxons('*:*')" +
                " MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:PARASITE_OF]->resourceSpecimen-[:CLASSIFIED_AS]-resourceTaxon" +
                " RETURN taxon.name";
        result = engine.execute(query);
        Set<String> actualParasites = new HashSet<String>();
        for (Map<String, Object> row : result) {
            actualParasites.add((String) row.get("taxon.name"));
        }

        assertThat(actualParasites.size() > 0, is(true));
        for (String unlikelyParasite : unlikelyParasites()) {
            assertThat(actualParasites, not(hasItem(unlikelyParasite)));
        }

        // Trypanorhyncha (kind of tapeworms) are typically parasites, not prey
        query = "START resourceTaxon = node:taxons(name='Trypanorhyncha')" +
                " MATCH taxon<-[:CLASSIFIED_AS]-specimen-[r:PREYS_UPON]->resourceSpecimen-[:CLASSIFIED_AS]-resourceTaxon" +
                " RETURN specimen.externalId + type(r) + resourceSpecimen.externalId as `resourceExternalId`";
        result = engine.execute(query);
        Set<String> actualPrey = new HashSet<String>();
        for (Map<String, Object> row : result) {
            actualPrey.add((String) row.get("resourceExternalId"));
        }

        assertThat(actualPrey.size(), is(0));
    }


    // see https://github.com/jhpoelen/eol-globi-data/issues/67
    private String[] unlikelyParasites() {
        return new String[]{"Tringa semipalmata",
                "Calidris mauri",
                "Larus occidentalis",
                "Egretta tricolor",
                "Melanitta perspicillata",
                "Egretta thula",
                "Charadrius semipalmatus",
                "Limnodromus griseus",
                "Thalasseus maximus",
                "Egretta rufescens",
                "Mergus serrator",
                "Larus delawarensis",
                "Podilymbus podiceps",
                "Pandion haliaetus",
                "Larus canus",
                "Limosa fedoa",
                "Calidris minutilla",
                "Numenius americanus",
                "Tringa melanoleuca",
                "Butorides virescens",
                "Ardea alba",
                "Sterna forsteri",
                "Thalasseus elegans",
                "Podiceps nigricollis",
                "Calidris alpina",
                "Gavia immer",
                "Hydroprogne caspia",
                "Larus californicus",
                "Bucephala albeola",
                "Pelecanus occidentalis",
                "Chroicocephalus philadelphia",
                "Megaceryle alcyon",
                "Nycticorax nycticorax",
                "Pluvialis squatarola",
                "Fulica americana",
                "Recurvirostra americana",
                "Numenius phaeopus",
                "Phalacrocorax penicillatus",
                "Paralichthys californicus",
                "Porichthys myriaster",
                "Leptocottus armatus",
                "Ilypnus gilberti",
                "Pleuronichthys guttulatus",
                "Gillichthys mirabilis",
                "Fundulus parvipinnis",
                "Syngnathus leptorhynchus",
                "Quietula y-cauda",
                "Clevelandia ios",
                "Acanthogobius flavimanus",
                "Chionista fluctifraga",
                "Chione californiensis",
                "Tagelus subteres",
                "Protothaca staminea",
                "Tagelus californianus",
                "Tagelus affinis",
                "Macoma nasuta",
                "Tripolium pannonicum",
                "Polydora nuchalis"};

    }
}
