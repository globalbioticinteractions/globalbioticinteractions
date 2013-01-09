package org.trophic.graph.data.taxon;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Transaction;
import org.trophic.graph.data.BaseImporter;
import org.trophic.graph.data.NodeFactory;
import org.trophic.graph.data.StudyImporterException;

import java.io.IOException;

public class TaxonomyImporter extends BaseImporter {

    private static final Log LOG = LogFactory.getLog(TaxonomyImporter.class);
    public static final int BATCH_TRANSACTION_SIZE = 10000;
    private int counter;
    private StopWatch stopwatch;
    private Transaction currentTransaction;

    private TaxonParser parser;

    private TaxonReaderFactory taxonReaderFactory;

    public TaxonomyImporter(NodeFactory nodeFactory) {
        this(nodeFactory, new OboParser(), new OboTaxonReaderFactory());
    }

    public TaxonomyImporter(NodeFactory nodeFactory, TaxonParser taxonParser, TaxonReaderFactory taxonReaderFactory) {
        super(nodeFactory);
        this.parser = taxonParser;
        this.taxonReaderFactory = taxonReaderFactory;
        stopwatch = new StopWatch();
        currentTransaction = null;
    }

    public TaxonParser getParser() {
        return parser;
    }

    public void importTaxonTerm(TaxonTerm term) throws StudyImporterException {
        if (term.getId() == null) {
            throw new StudyImporterException("missing mandatory field id in term with name [" + term.getName() + "]");
        }

        if (getCurrentTransaction() == null) {
            setCurrentTransaction(nodeFactory.getGraphDb().beginTx());
        }
        nodeFactory.createTaxonNoTransaction(term.getName(), term.getId());
        count();
        if (getCounter() % BATCH_TRANSACTION_SIZE == 0) {
            if (getCurrentTransaction() != null) {
                getCurrentTransaction().success();
                getCurrentTransaction().finish();
            }
            StopWatch stopwatch = getStopwatch();
            stopwatch.stop();
            double avg = 1000.0 * BATCH_TRANSACTION_SIZE / (stopwatch.getTime() + 1);
            String format = formatProgressString(avg);
            LOG.info(format);
            stopwatch.reset();
            stopwatch.start();
            setCurrentTransaction(nodeFactory.getGraphDb().beginTx());
        }
    }

    protected String formatProgressString(double avg) {
        return String.format("%d (%.1f%%), %.1f terms/s", getCounter(), 100.0 * (float) getCounter() / (float) getParser().getExpectedMaxTerms(), avg);
    }

    private void count() {
        this.counter++;
    }

    public void doImport() throws StudyImporterException {
        getStopwatch().reset();
        getStopwatch().start();
        setCounter(0);
        try {
            getParser().parse(taxonReaderFactory.createReader(), new TaxonTermListener() {
                @Override
                public void notifyTerm(TaxonTerm term) {
                    try {
                        importTaxonTerm(term);
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
