package org.eol.globi.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterFactory;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.db.GraphService;
import org.eol.globi.export.GraphExporter;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderFactoryImpl;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.EcoRegionFinderProxy;
import org.eol.globi.service.TaxonPropertyEnricherFactory;
import org.eol.globi.service.TaxonPropertyLookupServiceException;
import org.neo4j.graphdb.GraphDatabaseService;

public class Normalizer {
    private static final Log LOG = LogFactory.getLog(Normalizer.class);

    private EcoRegionFinder ecoRegionFinder = null;

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new Normalizer().normalize();
    }

    public void normalize() throws StudyImporterException {
        normalize("./");
    }

    private EcoRegionFinder getEcoRegionFinder() {
        if (null == ecoRegionFinder) {
            ecoRegionFinder = new EcoRegionFinderProxy(new EcoRegionFinderFactoryImpl().createAll());
        }
        return ecoRegionFinder;
    }

    public void setEcoRegionFinder(EcoRegionFinder finder) {
        this.ecoRegionFinder = finder;
    }

    public void normalize(String baseDir) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService(baseDir);
        importData(graphService);
        try {
            new Linker().linkTaxa(graphService);
        } catch (TaxonPropertyLookupServiceException e) {
            LOG.warn("failed to link taxa", e);
        }
        exportData(graphService, baseDir);
        graphService.shutdown();
        ecoRegionFinder.shutdown();
    }


    protected void exportData(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        new GraphExporter().export(graphService, baseDir);
    }


    private void importData(GraphDatabaseService graphService) {
        NodeFactory factory = new NodeFactory(graphService, new TaxonServiceImpl(TaxonPropertyEnricherFactory.createTaxonEnricher(), new TaxonNameCorrector(), graphService));
        for (Class importer : StudyImporterFactory.getAvailableImporters()) {
            try {
                importData(importer, factory);
            } catch (StudyImporterException e) {
                LOG.error("problem encountered while importing [" + importer.getName() + "]", e);
            }
        }
    }

    protected void importData(Class importer, NodeFactory factory) throws StudyImporterException {
        StudyImporter studyImporter = createStudyImporter(importer, factory);
        LOG.info("[" + importer + "] importing ...");
        studyImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
    }

    private StudyImporter createStudyImporter(Class<StudyImporter> studyImporter, NodeFactory factory) throws StudyImporterException {
        factory.setEcoRegionFinder(getEcoRegionFinder());
        factory.setDoiResolver(new DOIResolverImpl());
        ParserFactory parserFactory = new ParserFactoryImpl();
        StudyImporter importer = new StudyImporterFactory(parserFactory, factory).instantiateImporter(studyImporter);
        importer.setLogger(new StudyImportLogger(factory));
        return importer;
    }

}