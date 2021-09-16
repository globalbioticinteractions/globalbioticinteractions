package org.globalbioticinteractions.elton;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.Version;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryImpl;
import org.eol.globi.tool.Cmd;
import org.eol.globi.tool.CmdExport;
import org.eol.globi.tool.CmdGenerateReport;
import org.eol.globi.tool.CmdImportDatasets;
import org.eol.globi.tool.CmdIndexTaxa;
import org.eol.globi.tool.CmdIndexTaxonStrings;
import org.eol.globi.tool.CmdInterpretTaxa;
import org.eol.globi.tool.CmdOptionConstants;
import org.eol.globi.tool.CmdUtil;
import org.eol.globi.tool.Factories;
import org.eol.globi.tool.NodeFactoryFactory;
import org.eol.globi.tool.NodeFactoryFactoryTransactingOnDatasetNeo4j2;
import org.eol.globi.tool.NodeFactoryFactoryTransactingOnDatasetNeo4j3;
import org.eol.globi.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Elton4N {
    private static final Logger LOG = LoggerFactory.getLogger(Elton4N.class);
    private static final String OPTION_HELP = "h";

    private static final String ELTON_STEP_NAME_COMPILE = "compile";
    private static final String ELTON_STEP_NAME_LINK = "link";
    private static final String ELTON_STEP_NAME_PACKAGE = "package";

    public static void main(final String[] args) throws StudyImporterException, ParseException {
        String o = Version.getVersionInfo(Elton4N.class);
        LOG.info(o);
        CommandLine cmdLine = parseOptions(args);
        if (cmdLine.hasOption(OPTION_HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar elton4n-[VERSION].jar", getOptions());
        } else {
            try {
                new Elton4N().run(cmdLine);
            } catch (Throwable th) {
                LOG.error("failed to run GloBI indexer with [" + StringUtils.join(args, " ") + "]", th);
                throw th;
            }
        }
    }


    private static CommandLine parseOptions(String[] args) throws ParseException {
        CommandLineParser parser = new BasicParser();
        return parser.parse(getOptions(), args);
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(CmdOptionConstants.OPTION_DATASET_DIR, true, "specifies (input) datasets location");
        options.addOption(CmdOptionConstants.OPTION_EXPORT_DIR, true, "specifies data export location");
        options.addOption(CmdOptionConstants.OPTION_NEO4J_VERSION, true, "specifies version of Neo4j to use");
        options.addOption(CmdOptionConstants.OPTION_TAXON_CACHE_PATH, true, "specifies location of taxon cache to use");
        options.addOption(CmdOptionConstants.OPTION_TAXON_MAP_PATH, true, "specifies location of taxon map to use");

        Option helpOpt = new Option(OPTION_HELP, "help", false, "print this help information");
        options.addOption(helpOpt);
        return options;
    }

    private void run(CommandLine cmdLine) throws StudyImporterException {

        final String neo4jVersion = cmdLine == null
                ? "2"
                : cmdLine.getOptionValue(CmdOptionConstants.OPTION_NEO4J_VERSION, "2");

        importWithVersion(cmdLine, neo4jVersion);
    }

    private void importWithVersion(CommandLine cmdLine, String neo4jVersion) throws StudyImporterException {
        Factories factoriesNeo4j = new Factories() {
            final GraphServiceFactory factory =
                    new GraphServiceFactoryImpl(cmdLine.getOptionValue(CmdOptionConstants.OPTION_GRAPHDB_DIR,"./"));

            final NodeFactoryFactory nodeFactoryFactory = StringUtils.equals("2", neo4jVersion)
                    ? new NodeFactoryFactoryTransactingOnDatasetNeo4j2(factory)
                    : new NodeFactoryFactoryTransactingOnDatasetNeo4j3(factory);

            @Override
            public GraphServiceFactory getGraphServiceFactory() {
                return factory;
            }

            @Override
            public NodeFactoryFactory getNodeFactoryFactory() {
                return nodeFactoryFactory;
            }
        };


        try {
            String datasetDir = CmdUtil.getDatasetDir(cmdLine);
            GraphServiceFactory graphServiceFactory = factoriesNeo4j.getGraphServiceFactory();

            List<Cmd> steps = new ArrayList<>();

            LOG.info("processing steps: [" + StringUtils.join(cmdLine.getArgList(), ", ") + "]");

            if (cmdLine.getArgList().isEmpty() || cmdLine.getArgList().contains(ELTON_STEP_NAME_COMPILE)) {
                steps.add(new CmdImportDatasets(
                        factoriesNeo4j.getNodeFactoryFactory(),
                        graphServiceFactory,
                        datasetDir
                ));
            }

            if (cmdLine.getArgList().isEmpty() || cmdLine.getArgList().contains(ELTON_STEP_NAME_LINK)) {
                String taxonCachePath = cmdLine.getOptionValue(
                        CmdOptionConstants.OPTION_TAXON_CACHE_PATH,
                        "taxonCache.tsv.gz"
                );
                String taxonMapPath = cmdLine.getOptionValue(
                        CmdOptionConstants.OPTION_TAXON_MAP_PATH,
                        "taxonMap.tsv.gz"
                );
                steps.addAll(Arrays.asList(
                        new CmdInterpretTaxa(
                                graphServiceFactory,
                                taxonCachePath,
                                taxonMapPath
                        ),
                        new CmdIndexTaxa(graphServiceFactory),
                        new CmdIndexTaxonStrings(graphServiceFactory),
                        new CmdGenerateReport(graphServiceFactory.getGraphService())
                ));
            }
            if (cmdLine.getArgList().isEmpty() || cmdLine.getArgList().contains(ELTON_STEP_NAME_PACKAGE)) {
                String exportDir = cmdLine.getOptionValue(CmdOptionConstants.OPTION_EXPORT_DIR, ".");
                steps.add(new CmdExport(graphServiceFactory, exportDir));

            }

            for (Cmd step : steps) {
                step.run();
            }

        } finally {
            HttpUtil.shutdown();
        }
    }


}