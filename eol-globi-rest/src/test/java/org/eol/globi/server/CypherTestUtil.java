package org.eol.globi.server;

import org.eol.globi.data.NodeFactoryNeo4j2;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.taxon.NonResolvingTaxonIndexNeo4j2;
import org.eol.globi.tool.LinkerTaxonIndexNeo4j2;
import org.eol.globi.tool.CmdGenerateReportNeo4j2;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.NodeIdCollectorNeo4j2;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.helpers.NOPLogger;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CypherTestUtil {
    public static final String CYPHER_RESULT = "{\n" +
            "  \"columns\" : [ \"" + ResultField.TARGET_TAXON_NAME.getLabel() + "\", \"" + ResultField.LATITUDE.getLabel() + "\", \"" + ResultField.LONGITUDE.getLabel() + "\", \"" + ResultField.ALTITUDE.getLabel() + "\", \"" + ResultField.STUDY_TITLE.getLabel() + "\", \"" + ResultField.EVENT_DATE.getLabel() + "\", \"tmp_and_unique_specimen_id\", \"predator_life_stage\", \"prey_life_stage\", \"predator_body_part\", \"prey_body_part\", \"predator_physiological_state\", \"prey_physiological_state\", \"" + ResultField.SOURCE_TAXON_NAME.getLabel() + "\", \"" + ResultField.INTERACTION_TYPE.getLabel() + "\" ],\n" +
            "  \"data\" : [ [ \"Pomatomus saltatrix\", 39.76, -98.5, null, \"SPIRE\", null, 524716, null, null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
            "[ \"Lagodon rhomboides\", 28.626777, -96.104312, 0.7, \"Akin et al 2006\", 907365600000, 236033, null, null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
            "[ \"Centropomus undecimalis\", 26.823367, -82.271067, 0.0, \"Blewett 2006\", 984584100000, 217081, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
            "[ \"Centropomus undecimalis\", 26.823367, -82.271067, 0.0, \"Blewett 2006\", 984584100000, 217081, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], [ \"Centropomus undecimalis\", 26.688167, -82.245667, 0.0, \"Blewett 2006\", 971287200000, 216530, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ] ]\n" +
            "}";

    public static void validate(CypherQuery cypherQuery, GraphDatabaseService graphDatabaseService) {
        try(Transaction tx = graphDatabaseService.beginTx()) {
            File cacheDir = new File("target/reportGeneration" + UUID.randomUUID());
            new NodeFactoryNeo4j2(graphDatabaseService, cacheDir);
            new NonResolvingTaxonIndexNeo4j2(graphDatabaseService);
            new LinkerTaxonIndexNeo4j2(new GraphServiceFactoryProxy(graphDatabaseService), new NodeIdCollectorNeo4j2()).index();
            CmdGenerateReportNeo4j2 reportGenerator = new CmdGenerateReportNeo4j2();
            reportGenerator.setCacheDir(cacheDir.getAbsolutePath());
            reportGenerator.setGraphServiceFactory(new GraphServiceFactory() {
                @Override
                public GraphDatabaseService getGraphService() {
                    return graphDatabaseService;
                }

                @Override
                public void close() throws Exception {

                }
            });


            reportGenerator.run(NOPLogger.NOP_LOGGER);
            Map<String, Object> params =
                    cypherQuery.getParams() == null
                            ? Collections.emptyMap()
                            : new HashMap<>(cypherQuery.getParams());
            try {
                graphDatabaseService.execute(cypherQuery.getVersionedQuery(), params);
            } catch (NullPointerException ex) {
                // encountered nullpointer exceptions were caused by initialization of graph database
                throw ex;
            } catch (RuntimeException ex) {
                // for some reason lucene queries like "node:taxons('externalId:\"NCBI:9606\"') "
                // work fine in cypher query, but cause parse exception when running programatically
                if (!ex.getMessage().contains("Encountered \" \":\" \": \"\"")) {
                    throw ex;
                }
            }
        }
    }
}
