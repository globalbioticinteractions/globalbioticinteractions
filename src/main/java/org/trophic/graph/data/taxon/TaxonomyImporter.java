package org.trophic.graph.data.taxon;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.trophic.graph.data.NodeFactory;
import org.trophic.graph.data.StudyImporterException;

import java.io.IOException;

public class TaxonomyImporter  {

    private static final Log LOG = LogFactory.getLog(TaxonomyImporter.class);
    public static final int BATCH_TRANSACTION_SIZE = 10000;
    private int counter;
    private StopWatch stopwatch;

    private TaxonParser parser;

    private TaxonReaderFactory taxonReaderFactory;

    private final TaxonLookupServiceImpl taxonLookupService;

    public TaxonomyImporter(NodeFactory nodeFactory) {
        this(new OboParser(), new OboTaxonReaderFactory());
    }

    public TaxonomyImporter(TaxonParser taxonParser, TaxonReaderFactory taxonReaderFactory) {
        this.parser = taxonParser;
        this.taxonReaderFactory = taxonReaderFactory;
        this.taxonLookupService = new TaxonLookupServiceImpl();
        stopwatch = new StopWatch();
    }

    public TaxonLookupService getTaxonLookupService() {
        return taxonLookupService;
    }

    public TaxonParser getParser() {
        return parser;
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
            getParser().parse(taxonReaderFactory.createReader(), new TaxonImportListener() {
                @Override
                public void addTerm(String name, long id) {
                    taxonLookupService.addTerm(name, id);
                    count();
                    if (getCounter() % BATCH_TRANSACTION_SIZE == 0) {
                        StopWatch stopwatch = getStopwatch();
                        stopwatch.stop();
                        double avg = 1000.0 * BATCH_TRANSACTION_SIZE / (stopwatch.getTime() + 1);
                        String format = formatProgressString(avg);
                        LOG.info(format);
                        stopwatch.reset();
                        stopwatch.start();
                    }
                }

                @Override
                public void start() {
                    taxonLookupService.start();
                }

                @Override
                public void finish() {
                    taxonLookupService.finish();
                }
            });
        } catch (IOException e) {
            throw new StudyImporterException("failed to import taxonomy", e);
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

}
