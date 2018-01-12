package org.eol.globi.taxon;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Taxon;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

public class TaxonomyImporter {
    private static final Log LOG = LogFactory.getLog(TaxonomyImporter.class);

    public static final int BATCH_TRANSACTION_SIZE = 250000;
    private int counter;
    private StopWatch stopwatch;

    private TaxonParser parser;

    private TaxonReaderFactory taxonReaderFactory;

    private final TaxonLookupService taxonLookupService;

    private final TaxonImportListener taxonImportListener;

    public TaxonomyImporter(TaxonParser taxonParser, TaxonReaderFactory taxonReaderFactory) {
        this.parser = taxonParser;
        this.taxonReaderFactory = taxonReaderFactory;
        TaxonLookupServiceImpl service = new TaxonLookupServiceImpl();
        this.taxonLookupService = service;
        this.taxonImportListener = service;
        stopwatch = new StopWatch();
    }

    public TaxonLookupService getTaxonLookupService() {
        return taxonLookupService;
    }

    public TaxonParser getParser() {
        return parser;
    }

    protected String formatProgressString(double avg) {
        return String.format("%d %.1f terms/s", getCounter(), avg);
    }

    private void count() {
        this.counter++;
    }

    public void doImport() throws StudyImporterException {
        getStopwatch().reset();
        getStopwatch().start();
        setCounter(0);
        try {
            Map<String, BufferedReader> allReaders = taxonReaderFactory.getAllReaders();
            for (Map.Entry<String, BufferedReader> entry : allReaders.entrySet()) {
                try {
                    parse(entry.getValue());
                } catch (IOException ex) {
                    throw new IOException("failed to read from [" + entry.getKey() + "]");
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to import taxonomy", e);
        }
    }

    private void parse(BufferedReader reader) throws IOException {
        getParser().parse(reader, new TaxonImportListener() {
            @Override
            public void addTerm(Taxon term) {
                taxonImportListener.addTerm(term);
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
                taxonImportListener.start();
            }

            @Override
            public void finish() {
                taxonImportListener.finish();
            }
        });
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
