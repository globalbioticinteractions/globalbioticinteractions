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
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterFactory;
import org.eol.globi.data.taxon.TaxonIndexImpl;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.db.GraphService;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.export.GraphExporter;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderFactoryImpl;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.EcoregionFinderProxy;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.util.HttpUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Normalizer {
    private static final Log LOG = LogFactory.getLog(Normalizer.class);
    public static final String OPTION_HELP = "h";
    public static final String OPTION_SKIP_IMPORT = "skipImport";
    public static final String OPTION_SKIP_EXPORT = "skipExport";
    public static final String OPTION_SKIP_LINK = "skipLink";
    public static final String OPTION_SKIP_REPORT = "skipReport";
    public static final String OPTION_USE_DARK_DATA = "useDarkData";

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
        options.addOption(OPTION_SKIP_REPORT, false, "skip report generation step");
        options.addOption(OPTION_USE_DARK_DATA, false, "use only dark datasets (requires permission)");
        Option helpOpt = new Option(OPTION_HELP, "help", false, "print this help information");
        options.addOption(helpOpt);
        return options;
    }

    public void run(CommandLine cmdLine) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService("./");
        try {

            if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_IMPORT)) {
                Collection<Class<? extends StudyImporter>> importers = StudyImporterFactory.getOpenImporters();
                if (shouldUseDarkData(cmdLine)) {
                    LOG.info("adding dark importers...");
                    ArrayList<Class<? extends StudyImporter>> list = new ArrayList<Class<? extends StudyImporter>>();
                    list.addAll(importers);
                    list.addAll(StudyImporterFactory.getDarkImporters());
                    importers = Collections.unmodifiableList(list);
                }
                importData(graphService, importers);
            } else {
                LOG.info("skipping data import...");
            }

            if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_LINK)) {
                linkTaxa(graphService);
            } else {
                LOG.info("skipping taxa linking ...");
            }

            if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_REPORT)) {
                new ReportGenerator(graphService).run();
            } else {
                LOG.info("skipping report generation ...");
            }

            if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_EXPORT)) {
                exportData(graphService, "./");
            } else {
                LOG.info("skipping data export...");
            }
        } finally {
            graphService.shutdown();
            HttpUtil.shutdown();
        }

    }

    protected boolean shouldUseDarkData(CommandLine cmdLine) {
        return cmdLine != null && cmdLine.hasOption(OPTION_USE_DARK_DATA);
    }

    private void linkTaxa(GraphDatabaseService graphService) {
        try {
            new LinkerGlobalNames().link(graphService);
        } catch (PropertyEnricherException e) {
            LOG.warn("Problem linking taxa using Global Names Resolver", e);
        }

        String ottFile = System.getProperty("ott.file");
        try {
            if (StringUtils.isNotBlank(ottFile)) {
                new LinkerOpenTreeOfLife().link(graphService, new OpenTreeTaxonIndex(new File(ottFile).toURI().toURL()));
            }
        } catch (MalformedURLException e) {
            LOG.warn("failed to link against OpenTreeOfLife", e);
        }

        new LinkerTaxonIndex().link(graphService);

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


    private void importData(GraphDatabaseService graphService, Collection<Class<? extends StudyImporter>> importers) {
        TaxonIndexImpl taxonService = new TaxonIndexImpl(PropertyEnricherFactory.createTaxonEnricher()
                , new TaxonNameCorrector(), graphService);
        NodeFactoryImpl factory = new NodeFactoryImpl(graphService, taxonService);
        for (Class<? extends StudyImporter> importer : importers) {
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

    protected void importData(Class<? extends StudyImporter> importer, NodeFactoryImpl factory) throws StudyImporterException {
        StudyImporter studyImporter = createStudyImporter(importer, factory);
        LOG.info("[" + importer + "] importing ...");
        studyImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
    }

    private StudyImporter createStudyImporter(Class<? extends StudyImporter> studyImporter, NodeFactoryImpl factory) throws StudyImporterException {
        factory.setEcoregionFinder(getEcoregionFinder());
        ParserFactory parserFactory = new ParserFactoryImpl();
        StudyImporter importer = new StudyImporterFactory(parserFactory, factory).instantiateImporter(studyImporter);
        if (importer.shouldCrossCheckReference()) {
            factory.setDoiResolver(new DOIResolverImpl());
        }

        importer.setLogger(new StudyImportLogger());
        return importer;
    }

}