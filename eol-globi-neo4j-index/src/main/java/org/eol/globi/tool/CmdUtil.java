package org.eol.globi.tool;

import org.apache.commons.cli.CommandLine;

public class CmdUtil {
    public static String getDatasetDir(CommandLine cmdLine) {
        return cmdLine == null
                ? "target/datasets"
                : cmdLine.getOptionValue(CmdOptionConstants.OPTION_DATASET_DIR, "target/datasets");
    }
}
