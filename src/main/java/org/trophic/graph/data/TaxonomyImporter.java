package org.trophic.graph.data;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Transaction;
import org.trophic.graph.obo.OboParser;
import org.trophic.graph.obo.OboTermListener;
import org.trophic.graph.obo.OboUtil;
import org.trophic.graph.obo.TaxonParser;
import org.trophic.graph.obo.TaxonTerm;

import java.io.BufferedReader;
import java.io.IOException;

public class TaxonomyImporter extends BaseImporter {

    private static final Log LOG = LogFactory.getLog(TaxonomyImporter.class);
    public static final int SAMPLE_SIZE = 10000;
    private int counter;
    private StopWatch stopwatch;
    private Transaction currentTransaction;

    private TaxonParser parser = new OboParser();

    private TaxonReaderFactory taxonReaderFactory = new TaxonReaderFactory() {
        @Override
        public BufferedReader createReader() throws IOException {
            return OboUtil.getDefaultBufferedReader();
        }
    };

    public TaxonParser getParser() {
        return parser;
    }

    public TaxonomyImporter(NodeFactory nodeFactory) {
        super(nodeFactory);
        stopwatch = new StopWatch();
        currentTransaction = null;
    }

    public void importOboTerm(TaxonTerm term) throws StudyImporterException {
        if (term.getId() == null) {
            throw new StudyImporterException("missing mandatory field id in term with name [" + term.getName() + "]");
        }

        if (getCurrentTransaction() == null) {
            setCurrentTransaction(nodeFactory.getGraphDb().beginTx());
        }
        nodeFactory.createTaxonNoTransaction(term.getName(), term.getId());
        count();
        if (getCounter() % SAMPLE_SIZE == 0) {
            if (getCurrentTransaction() != null) {
                getCurrentTransaction().success();
                getCurrentTransaction().finish();
            }
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
        return String.format("%d (%.1f%%), %.1f terms/s", getCounter(), 100.0 * (float) getCounter() / (float) OboParser.MAX_TERMS, avg);
    }

    private void count() {
        this.counter++;
    }

    public void doImport() throws StudyImporterException {
        getStopwatch().reset();
        getStopwatch().start();
        setCounter(0);
        try {
            getParser().parse(taxonReaderFactory.createReader(), new OboTermListener() {
                @Override
                public void notifyTermWithRank(TaxonTerm term) {
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
