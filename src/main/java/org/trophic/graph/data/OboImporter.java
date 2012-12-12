package org.trophic.graph.data;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.trophic.graph.obo.OboParser;
import org.trophic.graph.obo.OboTerm;
import org.trophic.graph.obo.OboTermListener;
import org.trophic.graph.obo.OboUtil;

import java.io.IOException;

public class OboImporter extends BaseImporter {

    public static final String URN_LSID_PREFIX = "NCBITaxon:";

    private static final Log LOG = LogFactory.getLog(OboImporter.class);
    public static final int SAMPLE_SIZE = 10000;
    private int counter;
    private StopWatch stopwatch;
    private Transaction currentTransaction;

    public OboImporter(NodeFactory nodeFactory) {
        super(nodeFactory);
        stopwatch = new StopWatch();
        currentTransaction = null;
    }

    public void importOboTerm(OboTerm term) throws StudyImporterException {
        if (term.getId() == null) {
            throw new StudyImporterException("missing mandatory field id in term with name [" + term.getName() + "]");
        }

        if (getCurrentTransaction() == null) {
            setCurrentTransaction(nodeFactory.getGraphDb().beginTx());
        }
        nodeFactory.createTaxonNoTransaction(term.getName(), term.getId());
        count();
        if (getCounter() % SAMPLE_SIZE == 0) {
            getCurrentTransaction().success();
            getCurrentTransaction().finish();
            StopWatch stopwatch = getStopwatch();
            stopwatch.stop();
            double avg = 1000.0 * SAMPLE_SIZE / (stopwatch.getTime() + 1);
            String format = formatProgressString(avg);
            System.out.println(format);
            stopwatch.reset();
            stopwatch.start();
            setCurrentTransaction(nodeFactory.getGraphDb().beginTx());
        }
    }

    protected String formatProgressString(double avg) {
        return String.format("%d (%.1f%%), %.1f terms/s", getCounter(), 100.0*(float) getCounter() / (float) OboParser.MAX_TERMS, avg);
    }

    private void count() {
        this.counter++;
    }

    public void doImport() throws StudyImporterException {

        GraphDatabaseService inserter = BatchInserters.batchDatabase("target/batchinserter-example");

        OboParser parser = new OboParser();
        getStopwatch().reset();
        getStopwatch().start();
        setCounter(0);
        try {
            parser.parse(OboUtil.getDefaultBufferedReader(), new OboTermListener() {
                @Override
                public void notifyTermWithRank(OboTerm term) {
                    try {
                        importOboTerm(term);
                    } catch (StudyImporterException e) {
                        LOG.warn("failed to import term with id: [" + term.getId() + "]");
                    }
                }
            });
        } catch (IOException e) {
            throw new StudyImporterException("failed to import taxonomy", e);
        } finally {
            getCurrentTransaction().success();
            getCurrentTransaction().finish();
            setCurrentTransaction(null);
        }
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public StopWatch getStopwatch() {
        return stopwatch;
    }

    public Transaction getCurrentTransaction() {
        return currentTransaction;
    }

    public void setCurrentTransaction(Transaction currentTransaction) {
        this.currentTransaction = currentTransaction;
    }
}
