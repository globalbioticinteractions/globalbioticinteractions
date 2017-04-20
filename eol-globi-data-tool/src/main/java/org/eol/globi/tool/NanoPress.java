package org.eol.globi.tool;

import net.trustyuri.TrustyUriUtils;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.db.GraphService;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class NanoPress {
    private static final Log LOG = LogFactory.getLog(NanoPress.class);

    public static void main(final String[] args) throws IOException {
        CommandLineParser parser = new BasicParser();
        Option inputOpt = new Option("i", "input", true, "location of neo4j db");
        inputOpt.setRequired(true);
        Option outputOpt = new Option("o", "input", true, "output location for nanopubs");
        outputOpt.setRequired(true);

        Options options = new Options();
        options.addOption(inputOpt);
        options.addOption(outputOpt);

        CommandLine cmdLine;
        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(NanoPress.class.getSimpleName(), options);
            System.exit(1);
            return;
        }

        String inputFilePath = cmdLine.getOptionValue("input");
        String outputFilePath = cmdLine.getOptionValue("output");

        LOG.info("reading from neo4j: [" + inputFilePath + "], writing nanopubs to [" + outputFilePath + "]");

        pressNanopubs(inputFilePath, outputFilePath);
    }

    private static void pressNanopubs(String inputFilePath, String outputFilePath) throws IOException {
        final GraphDatabaseService graphService = GraphService.getGraphService(inputFilePath);

        File pubDir = new File(outputFilePath);
        FileUtils.forceMkdir(pubDir);

        List<Linker> linkers = Arrays.asList(new IndexInteractions(graphService),
                new LinkerTrustyNanoPubs(graphService, nanopub -> {
                    String code = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
                    OutputStream os = null;
                    if (StringUtils.isNotBlank(code)) {
                        try {
                            os = new GZIPOutputStream(new FileOutputStream(new File(pubDir, code + ".trig.gz")));
                        } catch (IOException e) {
                            LOG.error("failed to produce nanopub file for [" + code + "]", e);
                        }
                    }
                    return os == null ? new NullOutputStream() : os;
                }));
        linkers.forEach(LinkUtil::doTimedLink);
    }

}