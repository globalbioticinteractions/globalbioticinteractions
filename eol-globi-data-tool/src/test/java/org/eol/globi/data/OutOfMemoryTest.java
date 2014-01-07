package org.eol.globi.data;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.impl.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.assertThat;


/**
 * Tests limits of neo4j / cypher / lucene
 * <p/>
 * Current understanding - cypher does not handle streaming results very well.
 * It seems to load an entire result set into memory prior to returning it.
 */
public class OutOfMemoryTest {

    private GraphDatabaseService graphDb;
    private String storeDir;
    private NodeFactory factory;
    public static final long MAX_TAXONS = 1000 * 1000;

    @Before
    public void start() {
        storeDir = "target/testing" + System.currentTimeMillis();
        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir);
        graphDatabaseBuilder.setConfig(MapUtil.stringMap("use_memory_mapped_buffers", "false",
                "dump_configuration", "true",
                "lucene_searcher_cache_size", "100"
        ));
        graphDb = graphDatabaseBuilder.newGraphDatabase();

        factory = new NodeFactory(graphDb, new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) throws IOException {
            }
        });
        System.out.println(getMemMsg());
        assertThat(countAllNodes(1), Is.is(1L));
    }

    @After
    public void stop() throws IOException {
        if (graphDb != null) {
            graphDb.shutdown();
        }
        FileUtils.deleteRecursively(new File(storeDir));
    }

    @Ignore
    @Test
    public void importAndBigQuery() {
        insertTaxons(factory, MAX_TAXONS);
        assertThat(countAllNodes(MAX_TAXONS), Is.is(MAX_TAXONS + 1));
    }


    @Ignore
    @Test
    public void importAndBigCypherQuery() {
        // this one fails, probably because of result set caching.
        insertTaxons(factory, MAX_TAXONS);
        ExecutionEngine engine = new ExecutionEngine(graphDb);
        ExecutionResult execute = engine.execute("START taxon = node:taxons('*:*') RETURN taxon LIMIT 1000");
        Iterator<Object> taxon = execute.columnAs("taxon");
        long count = 0;
        while (taxon.hasNext()) {
            taxon.next();
            if (count % 1000 == 0) {
                printProgress(MAX_TAXONS, count);
            }
            count++;
        }
        assertThat(count, Is.is(MAX_TAXONS + 1));
    }

    private void insertTaxons(NodeFactory factory, long maxTaxons) {
        Transaction tx = graphDb.beginTx();
        for (long i = 0; i < maxTaxons; i++) {
            if (i % 5000 == 0) {
                tx.success();
                tx.finish();
                tx = graphDb.beginTx();
                printProgress(maxTaxons, i);
            }
            factory.createTaxonNoTransaction(i + "taxon", "externalId" + i, null);
        }
        tx.success();
        tx.finish();
    }

    private long countAllNodes(long maxTaxons) {
        long count = 0;
        Iterable<Node> allNodes = graphDb.getAllNodes();
        for (Node node : allNodes) {
            count++;
            if (count % 1000 == 0) {
                printProgress(maxTaxons, count);
            }
        }
        return count;
    }

    private void printProgress(long maxTaxons, long i) {
        System.out.println(100.0 * i / maxTaxons + "% done " + getMemMsg());
    }

    private String getMemMsg() {
        Runtime runtime = Runtime.getRuntime();
        double freeMemory = runtime.freeMemory() / (1024.0 * 1024.0);
        double maxMemory = runtime.maxMemory() / (1024.0 * 1024.0);
        String msg = String.format("mem %.1f/%.1f MB free (%.1f%% free)", freeMemory, maxMemory, percentFree(runtime));
        return msg;
    }

    private float percentFree(Runtime runtime) {
        return 100.0f * (float) runtime.freeMemory() / (float) runtime.maxMemory();
    }
}
