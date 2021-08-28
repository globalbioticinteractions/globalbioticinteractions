package org.eol.globi.util;

import org.eol.globi.data.GraphDBTestCase;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;

public class NodeProcessorImplTest extends GraphDBTestCase {

    @Test
    public void processAll() {
        AtomicLong counter = new AtomicLong();
        new NodeProcessorImpl(getGraphDb(), 10000L, "title", "*")
        .process(new NodeListener() {
            @Override
            public void on(Node node) {
                counter.incrementAndGet();
            }
        });

        assertThat(counter.get(), Is.is(0L));
    }

}