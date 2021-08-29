package org.eol.globi.util;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;

public class NodeProcessorImplTest extends GraphDBTestCase {

    @Test
    public void processEmpty() {
        AtomicLong counter = processStudies(10000L);

        assertThat(counter.get(), Is.is(0L));
    }

    @Test
    public void processSingle() throws MalformedDOIException, NodeFactoryException {

        Study study = createRandomStudy();
        getNodeFactory()
                .getOrCreateStudy(study);

        AtomicLong counter = processStudies(10000L);

        assertThat(counter.get(), Is.is(1L));
    }

    @Test
    public void processMultipleBatches() throws MalformedDOIException, NodeFactoryException {


        long batchSize = 2L;

        for (int i=0; i < (batchSize * 10) + 1; i++) {
            Study study = createRandomStudy();
            getNodeFactory()
                    .getOrCreateStudy(study);
        }

        AtomicLong counter = processStudies(batchSize);

        assertThat(counter.get(), Is.is(21L));
    }

    public AtomicLong processStudies(long batchSize) {
        AtomicLong counter = new AtomicLong();
        new NodeProcessorImpl(getGraphDb(), batchSize, "title", "*", "studies")
                .process(new NodeListener() {
                    @Override
                    public void on(Node node) {
                        counter.incrementAndGet();
                    }
                });
        return counter;
    }

    public StudyImpl createRandomStudy() throws MalformedDOIException {
        String randomString = UUID.randomUUID().toString();
        return new StudyImpl(randomString, DOI.create("10.123/" + randomString), randomString);
    }

}