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
import org.eol.globi.db.GraphService;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.export.GraphExporter;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderFactoryImpl;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.EOLTaxonImageService;
import org.eol.globi.service.EcoregionFinderProxy;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.eol.globi.util.HttpUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.index.Index;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImageLinker {
    private static final Log LOG = LogFactory.getLog(ImageLinker.class);
    public static final String OPTION_HELP = "h";
    public static final String USE_NEO4J_SOURCE = "skipImport";

    public static void main(final String[] args) throws StudyImporterException, ParseException {
        CommandLine cmdLine = parseOptions(args);
        if (cmdLine.hasOption(OPTION_HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar eol-globi-data-tool-[VERSION]-jar-with-dependencies.jar", getOptions());
        } else {
            new ImageLinker().run(cmdLine);
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

    public void run(CommandLine cmdLine)  {
        final GraphDatabaseService graphService = GraphService.getGraphService("./");
        try {
            linkImages(graphService);
        } finally {
            graphService.shutdown();
            HttpUtil.shutdown();
        }

    }

    public void linkImages(GraphDatabaseService graphService) {
        ExecutionEngine engine = new ExecutionEngine(graphService);
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
            final String infoURL = taxonImage == null ? "" : taxonImage.getInfoURL();
            final String thumbnailURL = taxonImage == null ? "" : taxonImage.getThumbnailURL();
            final String imageURL = taxonImage == null ? "" : taxonImage.getImageURL();

            ExecutionResult execute = engine.execute("START taxon = node({nodeId})\n" +
                    "SET taxon.externalUrl={infoUrl}, taxon.imageUrl={imageUrl}, taxon.thumbnailUrl={thumbnailUrl}\n" +
                    "RETURN taxon.externalId, taxon.externalUrl, taxon.thumbnailUrl, taxon.imageUrl",
                    new HashMap<String, Object>() {{
                        put("nodeId", nodeId);
                        put("infoUrl", infoURL);
                        put("imageUrl", imageURL);
                        put("thumbnailUrl", thumbnailURL);
                    }});

            for (Map<String, Object> stringObjectMap : execute) {
                System.out.println(StringUtils.join(stringObjectMap.values(), "\t"));
            }

        }
    }

}