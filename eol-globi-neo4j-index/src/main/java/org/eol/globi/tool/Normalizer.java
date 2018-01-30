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
import org.eol.globi.Version;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.ParserFactoryLocal;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForGitHubData;
import org.eol.globi.db.GraphService;
import org.eol.globi.domain.Taxon;
import org.eol.globi.export.GraphExporterImpl;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderFactoryImpl;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.DOIResolverCache;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.service.EcoregionFinderProxy;
import org.eol.globi.taxon.NonResolvingTaxonIndex;
import org.eol.globi.taxon.ResolvingTaxonIndex;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.cache.CacheFactory;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.dataset.DatasetFinderLocal;
import org.neo4j.graphdb.GraphDatabaseService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Normalizer {
    private static final Log LOG = LogFactory.getLog(Normalizer.class);
    private static final String OPTION_HELP = "h";
    private static final String OPTION_SKIP_IMPORT = "skipImport";
    private static final String OPTION_SKIP_TAXON_CACHE = "skipTaxonCache";
    private static final String OPTION_SKIP_RESOLVE = "skipResolve";
    private static final String OPTION_SKIP_EXPORT = "skipExport";
    private static final String OPTION_SKIP_LINK_THUMBNAILS = "skipLinkThumbnails";
    private static final String OPTION_SKIP_LINK = "skipLink";
    private static final String OPTION_SKIP_REPORT = "skipReport";
    private static final String OPTION_USE_DARK_DATA = "useDarkData";
    private static final String OPTION_DATASET_DIR = "datasetDir";
    private static final String OPTION_SKIP_RESOLVE_CITATIONS = OPTION_SKIP_RESOLVE;

    private EcoregionFinder ecoregionFinder = null;

    public static void main(final String[] args) throws StudyImporterException, ParseException {
        String o = Version.getVersionInfo(Normalizer.class);
        LOG.info(o);
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
        options.addOption(OPTION_SKIP_TAXON_CACHE, false, "skip usage of taxon cache");
        options.addOption(OPTION_SKIP_RESOLVE, false, "skip taxon name resolve to external taxonomies");
        options.addOption(OPTION_SKIP_LINK_THUMBNAILS, false, "skip linking of names to thumbnails");
        options.addOption(OPTION_SKIP_LINK, false, "skip taxa cross-reference step");
        options.addOption(OPTION_SKIP_REPORT, false, "skip report generation step");
        options.addOption(OPTION_USE_DARK_DATA, false, "use only dark datasets (requires permission)");
        options.addOption(OPTION_DATASET_DIR, true, "specifies location of dataset cache");

        Option helpOpt = new Option(OPTION_HELP, "help", false, "print this help information");
        options.addOption(helpOpt);
        return options;
    }

    public void run(CommandLine cmdLine) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService("./");
        try {
            importDatasets(cmdLine, graphService);
            resolveAndLinkTaxa(cmdLine, graphService);
            generateReports(cmdLine, graphService);
            exportData(cmdLine, graphService);
        } finally {
            graphService.shutdown();
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

    private void generateReports(CommandLine cmdLine, GraphDatabaseService graphService) {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_REPORT)) {
            new ReportGenerator(graphService).run();
        } else {
            LOG.info("skipping report generation ...");
        }
    }

    private void importDatasets(CommandLine cmdLine, GraphDatabaseService graphService) throws StudyImporterException {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_IMPORT)) {
            String defaultValue = "target/datasets";
            String cacheDir = cmdLine == null ? defaultValue : cmdLine.getOptionValue(OPTION_DATASET_DIR, defaultValue);
            importData(graphService, cacheDir);
        } else {
            LOG.info("skipping data import...");
        }
    }

    private void resolveAndLinkTaxa(CommandLine cmdLine, GraphDatabaseService graphService) {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_RESOLVE_CITATIONS)) {
            LOG.info("resolving citations to DOIs ...");
            new LinkerDOI(graphService, new DOIResolverCache()).link();
            new LinkerDOI(graphService).link();
        } else {
            LOG.info("skipping citation resolving ...");
        }


        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_TAXON_CACHE)) {
            LOG.info("resolving names with taxon cache ...");
            final TaxonCacheService taxonCacheService = new TaxonCacheService("/taxa/taxonCache.tsv.gz", "/taxa/taxonMap.tsv.gz");
            try {
                ResolvingTaxonIndex index = new ResolvingTaxonIndex(taxonCacheService, graphService);
                index.setIndexResolvedTaxaOnly(true);

                TaxonFilter taxonCacheFilter = new TaxonFilter() {

                    private KnownBadNameFilter knownBadNameFilter = new KnownBadNameFilter();

                    @Override
                    public boolean shouldInclude(Taxon taxon) {
                        return taxon != null
                                && knownBadNameFilter.shouldInclude(taxon);
                    }
                };

                new NameResolver(graphService, index, taxonCacheFilter).resolve();

                LOG.info("adding same and similar terms for resolved taxa...");
                List<Linker> linkers = new ArrayList<>();
                linkers.add(new LinkerTermMatcher(graphService, taxonCacheService));
                appendOpenTreeTaxonLinker(graphService, linkers);
                linkers.forEach(LinkUtil::doTimedLink);
                LOG.info("adding same and similar terms for resolved taxa done.");

            } finally {
                taxonCacheService.shutdown();
            }
            LOG.info("resolving names with taxon cache done.");
        } else {
            LOG.info("skipping taxon cache ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_RESOLVE)) {
            new NameResolver(graphService, new NonResolvingTaxonIndex(graphService)).resolve();
            new TaxonInteractionIndexer(graphService).index();
        } else {
            LOG.info("skipping taxa resolving ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_LINK)) {
            List<Linker> linkers = new ArrayList<>();
            linkers.add(new LinkerTaxonIndex(graphService));
            linkers.forEach(LinkUtil::doTimedLink);
        } else {
            LOG.info("skipping linking ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_LINK_THUMBNAILS)) {
            LinkUtil.doTimedLink(new ImageLinker(graphService, null));
        } else {
            LOG.info("skipping linking of taxa to thumbnails ...");
        }
    }

    public void appendOpenTreeTaxonLinker(GraphDatabaseService graphService, List<Linker> linkers) {
        String ottUrl = System.getProperty("ott.url");
        try {
            if (StringUtils.isNotBlank(ottUrl)) {
                linkers.add(new LinkerOpenTreeOfLife(graphService, new OpenTreeTaxonIndex(new URI(ottUrl).toURL())));
            }
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.warn("failed to link against OpenTreeOfLife", e);
        }
    }

    private EcoregionFinder getEcoregionFinder() {
        if (null == ecoregionFinder) {
            ecoregionFinder = new EcoregionFinderProxy(new EcoregionFinderFactoryImpl().createAll());
        }
        return ecoregionFinder;
    }

    void setEcoregionFinder(EcoregionFinder finder) {
        this.ecoregionFinder = finder;
    }

    void exportData(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        new GraphExporterImpl().export(graphService, baseDir);
    }


    void importData(GraphDatabaseService graphService, String cacheDir) {
        NodeFactoryNeo4j factory = new NodeFactoryNeo4j(graphService);
        factory.setEcoregionFinder(getEcoregionFinder());
        factory.setDoiResolver(new DOIResolverImpl());
        try {
            CacheFactory cacheFactory = dataset -> new CacheLocalReadonly(dataset.getNamespace(), cacheDir);
            DatasetFinder finder = new DatasetFinderLocal(cacheDir, cacheFactory);
            StudyImporter importer = new StudyImporterForGitHubData(new ParserFactoryLocal(), factory, finder);
            importer.setDataset(new DatasetLocal());
            importer.setLogger(new StudyImportLogger());
            importer.importStudy();
        } catch (StudyImporterException e) {
            LOG.error("problem encountered while importing [" + StudyImporterForGitHubData.class.getName() + "]", e);
        }
        EcoregionFinder regionFinder = getEcoregionFinder();
        if (regionFinder != null) {
            regionFinder.shutdown();
        }
    }

}