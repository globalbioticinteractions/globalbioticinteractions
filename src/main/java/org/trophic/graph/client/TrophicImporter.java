package org.trophic.graph.client;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.trophic.graph.data.NodeFactory;
import org.trophic.graph.data.ParserFactory;
import org.trophic.graph.data.ParserFactoryImpl;
import org.trophic.graph.data.StudyImporter;
import org.trophic.graph.data.StudyImporterException;
import org.trophic.graph.data.StudyImporterFactory;
import org.trophic.graph.data.StudyLibrary;
import org.trophic.graph.data.taxon.EOLTaxonParser;
import org.trophic.graph.data.taxon.EOLTaxonReaderFactory;
import org.trophic.graph.data.taxon.TaxonLookupService;
import org.trophic.graph.data.taxon.TaxonomyImporter;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Study;
import org.trophic.graph.export.StudyExporter;
import org.trophic.graph.export.StudyExporterImpl;
import org.trophic.graph.export.StudyExporterPredatorPrey;
import org.trophic.graph.export.StudyExporterPredatorPreyEOL;
import org.trophic.graph.service.ExternalIdTaxonEnricher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrophicImporter {
    private static final Log LOG = LogFactory.getLog(TrophicImporter.class);

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new TrophicImporter().importExport();
    }

    public void importExport() throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService();
        TaxonLookupService taxonLookupService = buildTaxonomyLookupService();

        List<Study> studies = importData(graphService, taxonLookupService);
        enrichData(graphService);
        exportData(studies);

        taxonLookupService.destroy();
        graphService.shutdown();
    }

    private void enrichData(GraphDatabaseService graphService) throws StudyImporterException {
        matchAgainstExternalTaxonomies(graphService);
        // TODO image retrieval is very inefficient - need a better way to link to images.
        /*try {
            new TaxonImageEnricher(graphService).process();
        } catch (IOException e) {
            throw new StudyImporterException("failed to add image url information", e);
        }*/
    }

    private void matchAgainstExternalTaxonomies(GraphDatabaseService graphService) throws StudyImporterException {
        try {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            LOG.info("Matching taxons against external taxonomies starting...");
            new ExternalIdTaxonEnricher(graphService).process();
            stopwatch.stop();
            LOG.info("Matching taxons against external complete. Total duration: [" + stopwatch.getTime() / (60.0 * 1000.0) + "] minutes");
        } catch (IOException e) {
            throw new StudyImporterException("enriching unmatched nodes failed", e);
        }
    }

    private TaxonLookupService buildTaxonomyLookupService() throws StudyImporterException {
        TaxonomyImporter importer = new TaxonomyImporter(new EOLTaxonParser(), new EOLTaxonReaderFactory());
        LOG.info("Taxonomy import starting...");
        importer.doImport();
        LOG.info("Taxonomy import complete.");
        return importer.getTaxonLookupService();
    }

    public List<Study> importStudies(GraphDatabaseService graphService, TaxonLookupService taxonLookupService) throws StudyImporterException {
        return importData(graphService, taxonLookupService);
    }

    private ArrayList<Study> importData(GraphDatabaseService graphService, TaxonLookupService taxonLookupService) throws StudyImporterException {
        ArrayList<StudyLibrary.Study> studies = new ArrayList<StudyLibrary.Study>();
        StudyLibrary.Study[] availableStudies = StudyLibrary.Study.values();
        studies.addAll(Arrays.asList(availableStudies));

        ArrayList<Study> importedStudies = new ArrayList<Study>();

        for (StudyLibrary.Study study : studies) {
            StudyImporter studyImporter = createStudyImporter(graphService, study, taxonLookupService);
            LOG.info("study [" + study + "] importing ...");
            importedStudies.add(studyImporter.importStudy());
            LOG.info("study [" + study + "] imported.");
        }
        return importedStudies;
    }

    private void exportData(List<Study> importedStudies) throws StudyImporterException {
        try {
            export(importedStudies, "./export.csv", new StudyExporterImpl());
            export(importedStudies, "./exportPredatorTaxonPreyTaxon.csv", new StudyExporterPredatorPrey(GraphService.getGraphService()));
            export(importedStudies, "./exportPredatorTaxonPreyTaxonInteractionTypeEOL.csv", new StudyExporterPredatorPreyEOL(GraphService.getGraphService()));
        } catch (IOException e) {
            throw new StudyImporterException("failed to export result to csv file", e);
        }
    }

    private void export(List<Study> importedStudies, String exportPath, StudyExporter studyExporter) throws IOException {
        FileWriter writer = new FileWriter(exportPath, false);
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] started...");
        for (Study importedStudy : importedStudies) {
            boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
            studyExporter.exportStudy(importedStudy, writer, includeHeader);
        }
        writer.flush();
        writer.close();
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] complete.");
    }

    private StudyImporter createStudyImporter(GraphDatabaseService graphService, StudyLibrary.Study study, TaxonLookupService taxonLookupService) throws StudyImporterException {
        NodeFactory factory = new NodeFactory(graphService, taxonLookupService);
        ParserFactory parserFactory = new ParserFactoryImpl();
        return new StudyImporterFactory(parserFactory, factory).createImporterForStudy(study);
    }

}