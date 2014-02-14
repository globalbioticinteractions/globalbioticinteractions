package org.eol.globi.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.data.taxon.TaxonService;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.db.GraphService;
import org.eol.globi.service.TaxonPropertyEnricherFactory;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;

import java.io.File;
import java.util.Map;

public class Indexer {
    private static final Log LOG = LogFactory.getLog(Indexer.class);

    public static void main(final String[] commandLineArguments) throws StudyImporterException, NodeFactoryException {
        new Indexer().indexKnownTaxa();
    }

    public void indexKnownTaxa() throws StudyImporterException, NodeFactoryException {
        indexKnownTaxa("./");
    }

    public void indexKnownTaxa(String baseDir) throws StudyImporterException, NodeFactoryException {
        String previousDataDir = System.getProperty("previous.data.dir");
        if (StringUtils.isBlank(previousDataDir)) {
            throw new StudyImporterException("please specify -Dprevious.data.dir=[...] on the commandline");
        }
        File previousData = new File(previousDataDir);
        if (!previousData.exists() || !previousData.isDirectory()) {
            throw new StudyImporterException("database directory [" + previousData.getAbsolutePath() + "] does not exist");
        }

        final GraphDatabaseService previousGraphService = GraphService.getGraphService(previousData.getAbsolutePath() + File.separator);
        ExecutionEngine executionEngine = new ExecutionEngine(previousGraphService);

        LOG.info("previous taxon names retrieving ...");
        ExecutionResult results = executionEngine.execute("START taxon = node:taxons('*:*') MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:ORIGINALLY_DESCRIBED_AS]->origName RETURN distinct(origName.name) as name, origName.externalId? as externalId");
        LOG.info("previous taxon names retrieved.");

        final GraphDatabaseService freshGraphService = GraphService.getGraphService(baseDir);
        TaxonService taxonService = new TaxonServiceImpl(TaxonPropertyEnricherFactory.createTaxonEnricher()
                , new TaxonNameCorrector()
                , freshGraphService);

        LOG.info("taxon names indexing...");
        ResourceIterator<Map<String, Object>> iterator = results.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> next = iterator.next();
            String name = null;
            if (next.containsKey("name")) {
                name = (String) next.get("name");
            }
            String externalId = null;
            if (next.containsKey("externalId")) {
                externalId = (String) next.get("externalId");
            }
            taxonService.getOrCreateTaxon(name, externalId, null);
        }
        iterator.close();
        LOG.info("taxon names indexed.");
        freshGraphService.shutdown();
        previousGraphService.shutdown();
    }

}