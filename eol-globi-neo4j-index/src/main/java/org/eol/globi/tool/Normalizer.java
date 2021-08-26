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
import org.eol.globi.export.GraphExporterImpl;
import org.eol.globi.service.DOIResolverCache;
import org.eol.globi.taxon.NonResolvingTaxonIndex;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.HttpUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        Factories factoriesNeo4j2 = new Factories() {
            final GraphServiceFactory factory = new GraphServiceFactoryImpl("./");

            @Override
            public GraphServiceFactory getGraphServiceFactory() {
                return factory;
            }

            @Override
            public NodeFactoryFactory getNodeFactoryFactory() {
                return service -> new NodeFactoryNeo4j2(factory.getGraphService());
            }
        };

        GraphServiceFactory graphServiceFactory = factoriesNeo4j2.getGraphServiceFactory();
        try {
            importDatasets(cmdLine, graphServiceFactory, factoriesNeo4j2.getNodeFactoryFactory());
            resolveAndLinkTaxa(cmdLine, graphServiceFactory);
            generateReports(cmdLine, graphServiceFactory);
            exportData(cmdLine, graphServiceFactory.getGraphService());
        } finally {
            graphServiceFactory.clear();
            HttpUtil.shutdown();
        }

    }

    private void exportData(CommandLine cmdLine, GraphDatabaseService graphService) throws StudyImporterException {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_EXPORT)) {
            exportData(graphService, "./");
        } else {
            LOG.info("skipping data export...");
        }
    }

    private void generateReports(CommandLine cmdLine, GraphServiceFactory graphService) {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_REPORT)) {
            new ReportGenerator(graphService.getGraphService()).run();
        } else {
            LOG.info("skipping report generation ...");
        }
    }

    private void importDatasets(CommandLine cmdLine, GraphServiceFactory factory, NodeFactoryFactory nodeFactoryFactory) throws StudyImporterException {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_IMPORT)) {
            new CmdIndexDatasets(cmdLine, nodeFactoryFactory, factory).run();
        } else {
            LOG.info("skipping data import...");
        }
    }

    private void resolveAndLinkTaxa(CommandLine cmdLine, GraphServiceFactory graphServiceFactory) {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_RESOLVE_CITATIONS)) {
            LOG.info("resolving citations to DOIs ...");
            new LinkerDOI(new DOIResolverCache()).index(graphServiceFactory);
        } else {
            LOG.info("skipping citation resolving ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_TAXON_CACHE)) {
            final TaxonCacheService taxonCacheService = new TaxonCacheService(
                    "/taxa/taxonCache.tsv.gz",
                    "/taxa/taxonMap.tsv.gz");
            IndexerNeo4j taxonIndexer = new IndexerTaxa(taxonCacheService);
            taxonIndexer.index(graphServiceFactory);
        } else {
            LOG.info("skipping taxon cache ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_RESOLVE)) {
            final NonResolvingTaxonIndex taxonIndex = new NonResolvingTaxonIndex(graphServiceFactory.getGraphService());
            final IndexerNeo4j nameResolver = new NameResolver(taxonIndex);
            final IndexerNeo4j taxonInteractionIndexer = new TaxonInteractionIndexer();

            Arrays.asList(nameResolver, taxonInteractionIndexer)
                    .forEach(x -> x.index(graphServiceFactory));
        } else {
            LOG.info("skipping taxa resolving ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_LINK)) {
            List<IndexerNeo4j> linkers = new ArrayList<>();
            linkers.add(new LinkerTaxonIndex());
            linkers.forEach(x -> new IndexerTimed(x)
                    .index(graphServiceFactory));
        } else {
            LOG.info("skipping linking ...");
        }

    }

    void exportData(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        new GraphExporterImpl().export(graphService, baseDir);
    }


}