package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LinkUtil {
    private static final Log LOG = LogFactory.getLog(LinkUtil.class);

    public static void doTimedLink(Linker linker) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String linkName = Linker.class.getSimpleName();
        LOG.info(linkName + " started...");
        try {
            linker.link();
        } finally {
            stopWatch.stop();
            LOG.info(linkName + " completed in [" + stopWatch.getTime() / 1000 + "]s");
        }
    }
}
