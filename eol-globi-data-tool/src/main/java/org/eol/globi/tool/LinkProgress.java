package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.atomic.AtomicLong;

public class LinkProgress {
    private final LinkProgressListener listener;
    private StopWatch stopWatch = new StopWatch();
    private AtomicLong counter = new AtomicLong(0);
    private int reportInterval = 100;

    public LinkProgress(LinkProgressListener listener) {
        this.listener = listener;
    }

    public void start() {
        stopWatch.reset();
        counter.set(0);
        stopWatch.start();
    }

    public void progress() {
        counter.incrementAndGet();
        if (counter.get() % reportInterval == 0) {
            long time = stopWatch.getTime() / 1000;
            listener.onProgress(String.format("handled [%d] in [%d]s ([%.1f] /s)", counter.get(), time, (float) counter.get() / time));
        }

    }
}
