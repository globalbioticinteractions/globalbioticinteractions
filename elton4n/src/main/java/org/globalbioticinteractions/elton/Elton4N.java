package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdCompile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.codegen.docgen.manpage.ManPageGenerator;

import static java.lang.System.exit;

@CommandLine.Command(name = "elton4n",
        versionProvider = Elton4N.class,
        subcommands = {
                CmdCompile.class,
                CmdLink.class,
                CmdPackage.class,
                ManPageGenerator.class,
                CommandLine.HelpCommand.class
        },
        description = "compile, interpret, index, and repackage existing species interaction datasets using Neo4J",
        mixinStandardHelpOptions = true,
        subcommandsRepeatable = true
)

public class Elton4N implements CommandLine.IVersionProvider {

    public String[] getVersion() {
        return new String[]{getVersionString()};
    }

    public static String getVersionString() {
        String version = Elton4N.class.getPackage().getImplementationVersion();
        return org.apache.commons.lang.StringUtils.isBlank(version) ? "dev" : version;
    }

    public static void main(String[] args) {
        try {
            int exitCode = run(args);
            System.exit(exitCode);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            exit(1);
        }
    }

    public static int run(String[] args) {
        return new CommandLine(new Elton4N()).execute(args);
    }

}

