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
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphService;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.service.EOLTaxonImageService;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.HttpUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ImageLinker implements Linker {
    private static final Log LOG = LogFactory.getLog(ImageLinker.class);
    public static final String OPTION_HELP = "h";
    public static final String USE_NEO4J_SOURCE = "skipImport";

    private final PrintStream out;
    private final GraphDatabaseService graphDb;

    public ImageLinker(GraphDatabaseService graphService, PrintStream out) {
        this.graphDb = graphService;
        this.out = out;
    }

    public static void main(final String[] args) throws StudyImporterException, ParseException {
        CommandLine cmdLine = parseOptions(args);
        if (cmdLine.hasOption(OPTION_HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar eol-globi-data-tool-[VERSION]-jar-with-dependencies.jar", getOptions());
        } else {
            ImageLinker.run(cmdLine);
        }
    }


    protected static CommandLine parseOptions(String[] args) throws ParseException {
        CommandLineParser parser = new BasicParser();
        return parser.parse(getOptions(), args);
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(USE_NEO4J_SOURCE, false, "taxon ids from GloBI neo4j");
        Option helpOpt = new Option(OPTION_HELP, "help", false, "print this help information");
        options.addOption(helpOpt);
        return options;
    }

    private static void run(CommandLine cmdLine)  {
        final GraphDatabaseService graphService = GraphService.getGraphService("./");
        try {
            new ImageLinker(graphService, System.out).link();
        } finally {
            graphService.shutdown();
            HttpUtil.shutdown();
        }

    }

    @Override
    public void link() {
        ExecutionEngine engine = new ExecutionEngine(graphDb);
        ExecutionResult executionResult = engine.execute("START taxon = node:taxons('*:*')\n" +
                "WHERE not(has(taxon.thumbnailUrl)) AND has(taxon.externalId) AND taxon.externalId <> 'no:match'\n" +
                "RETURN id(taxon) as `id`, taxon.externalId as `externalId`");
        for (Map<String, Object> externalIdMap : executionResult) {
            final String externalId = (String) externalIdMap.get("externalId");
            final Long nodeId = (Long) externalIdMap.get("id");
            TaxonImage taxonImage = null;
            try {
                taxonImage = new EOLTaxonImageService().lookupImageForExternalId(externalId);
            } catch (IOException e) {
                LOG.warn("failed to lookup externalId [" + externalId + "]", e);
            }
            if (taxonImage == null) {
                if (out != null) {
                    out.println(StringUtils.join(Arrays.asList("EOL:12345", "", "", ""), "\t"));
                }
            } else {
                final String infoURL = taxonImage.getInfoURL() == null ? "" : taxonImage.getInfoURL();
                final String thumbnailURL = taxonImage.getThumbnailURL() == null ? "" : taxonImage.getThumbnailURL();
                final String imageURL = taxonImage.getImageURL() == null ? "" : taxonImage.getImageURL();

                ExecutionResult execute = engine.execute("START taxon = node({nodeId})\n" +
                                "SET taxon.externalUrl={infoUrl}, taxon.imageUrl={imageUrl}, taxon.thumbnailUrl={thumbnailUrl}\n" +
                                "RETURN taxon.externalId, taxon.externalUrl, taxon.thumbnailUrl, taxon.imageUrl",
                        new HashMap<String, Object>() {{
                            put("nodeId", nodeId);
                            put("infoUrl", infoURL);
                            put("imageUrl", imageURL);
                            put("thumbnailUrl", thumbnailURL);
                        }});

                if (out != null) {
                    for (Map<String, Object> stringObjectMap : execute) {
                        out.println(StringUtils.join(CSVTSVUtil.escapeValues(stringObjectMap.values()), "\t"));
                    }
                }
                if (execute != null && execute.iterator() != null) {
                    execute.iterator().close();
                }
            }

        }
    }

}