package org.eol.globi.tool;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.Version;
import org.eol.globi.data.NodeFactoryNeo4j2;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryImpl;
import org.eol.globi.service.DOIResolverCache;
import org.eol.globi.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Normalizer {
    private static final Logger LOG = LoggerFactory.getLogger(Normalizer.class);
    private static final String OPTION_HELP = "h";
    private static final String OPTION_SKIP_IMPORT = "skipImport";
    private static final String OPTION_SKIP_TAXON_CACHE = "skipTaxonCache";
    private static final String OPTION_SKIP_RESOLVE = "skipResolve";
    private static final String OPTION_SKIP_EXPORT = "skipExport";
    private static final String OPTION_SKIP_LINK_THUMBNAILS = "skipLinkThumbnails";
    private static final String OPTION_SKIP_LINK = "skipLink";
    private static final String OPTION_SKIP_REPORT = "skipReport";
    private static final String OPTION_SKIP_RESOLVE_CITATIONS = OPTION_SKIP_RESOLVE;

    public static void main(final String[] args) throws StudyImporterException, ParseException {
        String o = Version.getVersionInfo(Normalizer.class);
        LOG.info(o);
        CommandLine cmdLine = parseOptions(args);
        if (cmdLine.hasOption(OPTION_HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar eol-globi-data-tool-[VERSION]-jar-with-dependencies.jar", getOptions());
        } else {
            try {
                new Normalizer().run(cmdLine);
            } catch (Throwable th) {
                LOG.error("failed to run GloBI indexer with [" + StringUtils.join(args, " ") + "]", th);
                throw th;
            }
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
        options.addOption(OPTION_SKIP_TAXON_CACHE, false, "skip usage of taxon cache");
        options.addOption(OPTION_SKIP_RESOLVE, false, "skip taxon name query to external taxonomies");
        options.addOption(OPTION_SKIP_LINK_THUMBNAILS, false, "skip linking of names to thumbnails");
        options.addOption(OPTION_SKIP_LINK, false, "skip taxa cross-reference step");
        options.addOption(OPTION_SKIP_REPORT, false, "skip report generation step");
        options.addOption(CmdOptionConstants.OPTION_DATASET_DIR, true, "specifies location of dataset cache");

        Option helpOpt = new Option(OPTION_HELP, "help", false, "print this help information");
        options.addOption(helpOpt);
        return options;
    }

    public void run(CommandLine cmdLine) throws StudyImporterException {

        GraphServiceFactoryImpl graphServiceFactory
                = new GraphServiceFactoryImpl(new File(".").getAbsolutePath());

        try {
            indexDatasets(cmdLine, graphServiceFactory);
            processDatasets(cmdLine, graphServiceFactory);
        } finally {
            HttpUtil.shutdown();
        }

    }

    private void processDatasets(CommandLine cmdLine, GraphServiceFactoryImpl graphServiceFactory) throws StudyImporterException {
        GraphServiceFactory factory = new FactoriesBase(graphServiceFactory)
                .getGraphServiceFactory();

        resolveAndLinkTaxa(cmdLine, factory);

        generateReports(cmdLine, factory);

        exportData(cmdLine, factory);
    }

    private void exportData(CommandLine cmdLine, GraphServiceFactory factory) throws StudyImporterException {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_EXPORT)) {
            new CmdExport(factory).run();
        }
    }

    private void generateReports(CommandLine cmdLine, GraphServiceFactory graphServiceFactory1) {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_REPORT)) {
            new CmdGenerateReport(graphServiceFactory1.getGraphService()).run();
        } else {
            LOG.info("skipping report generation ...");
        }
    }

    private void indexDatasets(CommandLine cmdLine, GraphServiceFactory graphServiceFactory) throws StudyImporterException {
        Factories importerFactory = new FactoriesForDatasetImportNeo4jV2(graphServiceFactory);
        GraphServiceFactory graphDbFactory = importerFactory.getGraphServiceFactory();
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_IMPORT)) {
            new CmdImportDatasets(
                    importerFactory.getNodeFactoryFactory(),
                    graphDbFactory,
                    CmdUtil.getDatasetDir(cmdLine)
            ).run();
        } else {
            LOG.info("skipping data import...");
        }
    }

    private void resolveAndLinkTaxa(CommandLine cmdLine, GraphServiceFactory graphServiceFactory) throws StudyImporterException {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_RESOLVE_CITATIONS)) {
            LOG.info("resolving citations to DOIs ...");
            new LinkerDOI(graphServiceFactory, new DOIResolverCache()).index();
        } else {
            LOG.info("skipping citation resolving ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_TAXON_CACHE)) {
            new CmdInterpretTaxa(graphServiceFactory).run();
        } else {
            LOG.info("skipping taxon cache ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_RESOLVE)) {
            new CmdIndexTaxa(graphServiceFactory).run();
        } else {
            LOG.info("skipping taxa resolving ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_LINK)) {
            new CmdIndexTaxonStrings(graphServiceFactory).run();
        } else {
            LOG.info("skipping linking ...");
        }
    }

    public class FactoriesForDatasetImportNeo4jV2 extends FactoriesBase {
        FactoriesForDatasetImportNeo4jV2(GraphServiceFactory graphServiceFactory) {
            super(graphServiceFactory);
        }

        @Override
        public NodeFactoryFactory getNodeFactoryFactory() {
            return new NodeFactoryFactoryTransactingOnDatasetNeo4j2(this.getGraphServiceFactory());
        }

    }

    public class FactoriesForDatasetImportNeo4jV3 extends FactoriesBase {
        FactoriesForDatasetImportNeo4jV3(GraphServiceFactory graphServiceFactory) {
            super(graphServiceFactory);
        }

        @Override
        public NodeFactoryFactory getNodeFactoryFactory() {
            return new NodeFactoryFactoryTransactingOnDatasetNeo4j3(this.getGraphServiceFactory());
        }

    }

    public class FactoriesBase implements Factories {

        private final GraphServiceFactory factory;

        FactoriesBase(GraphServiceFactory factory) {
            this.factory = factory;
        }

        @Override
        public GraphServiceFactory getGraphServiceFactory() {
            return factory;
        }

        @Override
        public NodeFactoryFactory getNodeFactoryFactory() {
            return service -> new NodeFactoryNeo4j2(factory.getGraphService());
        }
    }

}