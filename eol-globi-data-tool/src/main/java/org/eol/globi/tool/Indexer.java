package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
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
import org.eol.globi.service.TaxonPropertyLookupServiceException;
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

        final GraphDatabaseService freshGraphService = GraphService.startNeo4j(baseDir);
        TaxonService taxonService = new TaxonServiceImpl(TaxonPropertyEnricherFactory.createTaxonEnricher()
                , new TaxonNameCorrector()
                , freshGraphService);
        indexUsingExternalIds(executionEngine, taxonService);
        indexUsingNamesWithNoExternalIds(executionEngine, taxonService);
        try {
            new Linker().linkTaxa(freshGraphService);
        } catch (TaxonPropertyLookupServiceException e) {
            LOG.warn("failed to link taxa", e);
        }

        freshGraphService.shutdown();
        previousGraphService.shutdown();
    }

    private void indexUsingNamesWithNoExternalIds(ExecutionEngine executionEngine, TaxonService taxonService) throws NodeFactoryException {
        String msgPrefix = "taxon names";
        String whereAndReturnClause = " WHERE has(origName.name) AND not(has(origName.externalId)) RETURN distinct(origName.name) as name";
        indexTaxonByProperty(executionEngine, taxonService, msgPrefix, whereAndReturnClause);
    }

    private void indexUsingExternalIds(ExecutionEngine executionEngine, TaxonService taxonService) throws NodeFactoryException {
        String msgPrefix = "taxon externalIds";
        String whereAndReturnClause = " WHERE has(origName.externalId) RETURN distinct(origName.externalId) as externalId";
        indexTaxonByProperty(executionEngine, taxonService, msgPrefix, whereAndReturnClause);
    }

    private void indexTaxonByProperty(ExecutionEngine executionEngine, TaxonService taxonService, String msgPrefix, String whereAndReturnClause) throws NodeFactoryException {
        LOG.info(msgPrefix + " retrieving ...");
        ExecutionResult results = executionEngine.execute("START taxon = node:taxons('*:*') MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:ORIGINALLY_DESCRIBED_AS]->origName " + whereAndReturnClause);
        LOG.info(msgPrefix + " retrieved.");


        LOG.info(msgPrefix + " indexing...");
        ResourceIterator<Map<String, Object>> iterator = results.iterator();
        int counter = 0;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
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
            counter++;
            if (counter % 100 == 0) {
                LOG.info("[" + counter + "] taxa indexed at [" + 1000.0 * counter / stopWatch.getTime() + "] names/s");
            }
        }
        stopWatch.stop();
        iterator.close();
        LOG.info(msgPrefix + " indexed in [" + stopWatch.getTime() / 1000.0 * 3600.0 + "] hours.");
    }

}