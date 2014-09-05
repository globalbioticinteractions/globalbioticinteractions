package org.eol.globi.tool;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterFactory;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.data.taxon.TaxonIndexImpl;
import org.eol.globi.db.GraphService;
import org.eol.globi.export.GraphExporter;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderFactoryImpl;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.EcoregionFinderProxy;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.service.PropertyEnricherException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;

public class Normalizer {
    private static final Log LOG = LogFactory.getLog(Normalizer.class);
    public static final String OPTION_HELP = "h";
    public static final String OPTION_SKIP_IMPORT = "skipImport";
    public static final String OPTION_SKIP_EXPORT = "skipExport";
    public static final String OPTION_SKIP_LINK = "skipLink";

    private EcoregionFinder ecoregionFinder = null;

    public static void main(final String[] args) throws StudyImporterException, ParseException {
        CommandLine cmdLine = parseOptions(args);
        if (cmdLine.hasOption(OPTION_HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar eol-globi-data-tool-[VERSION]-jar-with-dependencies.jar", getOptions());
        } else {
            new Normalizer().run(cmdLine);
        }
    }

    protected static CommandLine parseOptions(String[] args) throws ParseException {
        CommandLineParser parser = new BasicParser();
        return parser.parse(getOptions(), args);
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(OPTION_SKIP_IMPORT, false, "skip the import of all GloBI datasets");
        options.addOption(OPTION_SKIP_EXPORT, false, "skip the export for GloBI datasets to aggregated archives.");
        options.addOption(OPTION_SKIP_LINK, false, "skip taxa cross-reference step");
        Option helpOpt = new Option(OPTION_HELP, "help", false, "print this help information");
        options.addOption(helpOpt);
        return options;
    }

    public void run(CommandLine cmdLine) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService("./");
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_IMPORT)) {
            importData(graphService, getImporters());
            if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_LINK)) {
                try {
                    Linker linker = new Linker();
                    linker.linkToGlobalNames(graphService);

                    String ottFile = System.getProperty("ott.file");
                    if (StringUtils.isNotBlank(ottFile)) {
                        linker.linkToOpenTreeOfLife(graphService, new OpenTreeTaxonIndex(new File(ottFile).toURI().toURL()));
                    }
                } catch (PropertyEnricherException e) {
                    LOG.warn("failed to link taxa", e);
                } catch (MalformedURLException e) {
                    LOG.warn("failed to link against OpenTreeOfLife", e);
                }
            } else {
                LOG.info("skipping taxa linking ...");
            }
        } else {
            LOG.info("skipping data import...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_EXPORT)) {
            exportData(graphService, "./");
        } else {
            LOG.info("skipping data export...");
        }
        graphService.shutdown();
    }

    protected Collection<Class> getImporters() {
        return StudyImporterFactory.getAvailableImporters();
    }

    private EcoregionFinder getEcoregionFinder() {
        if (null == ecoregionFinder) {
            ecoregionFinder = new EcoregionFinderProxy(new EcoregionFinderFactoryImpl().createAll());
        }
        return ecoregionFinder;
    }

    public void setEcoregionFinder(EcoregionFinder finder) {
        this.ecoregionFinder = finder;
    }

    protected void exportData(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        new GraphExporter().export(graphService, baseDir);
    }


    private void importData(GraphDatabaseService graphService, Collection<Class> importers) {
        TaxonIndexImpl taxonService = new TaxonIndexImpl(PropertyEnricherFactory.createTaxonEnricher()
                , new TaxonNameCorrector(), graphService);
        NodeFactory factory = new NodeFactory(graphService, taxonService);
        for (Class importer : importers) {
            try {
                importData(importer, factory);
            } catch (StudyImporterException e) {
                LOG.error("problem encountered while importing [" + importer.getName() + "]", e);
            }
        }
        EcoregionFinder regionFinder = getEcoregionFinder();
        if (regionFinder != null) {
            regionFinder.shutdown();
        }
    }

    protected void importData(Class importer, NodeFactory factory) throws StudyImporterException {
        StudyImporter studyImporter = createStudyImporter(importer, factory);
        LOG.info("[" + importer + "] importing ...");
        studyImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
    }

    private StudyImporter createStudyImporter(Class<StudyImporter> studyImporter, NodeFactory factory) throws StudyImporterException {
        factory.setEcoregionFinder(getEcoregionFinder());
        factory.setDoiResolver(new DOIResolverImpl());
        ParserFactory parserFactory = new ParserFactoryImpl();
        StudyImporter importer = new StudyImporterFactory(parserFactory, factory).instantiateImporter(studyImporter);
        importer.setLogger(new StudyImportLogger(factory));
        return importer;
    }

}