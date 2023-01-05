package org.eol.globi.util;

import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyImpl;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class NodeProcessorImplNeo4j2Test extends GraphDBNeo4jTestCase {

    @Test
    public void processEmpty() {
        AtomicLong counter = processStudies(10000L, getNodeIdCollector());
        assertThat(counter.get(), Is.is(0L));
    }

    @Test
    public void processSingle() throws MalformedDOIException, NodeFactoryException {

        StudyImpl randomStudy = createRandomStudy();
        getNodeFactory().getOrCreateStudy(randomStudy);

        assertNotNull(getNodeFactory().findStudy(randomStudy));

        AtomicLong counter = processStudies(10000L, getNodeIdCollector());

        assertThat(counter.get(), Is.is(1L));
    }

    @Test
    public void processMultipleBatches() throws MalformedDOIException, NodeFactoryException {
        long batchSize = 2L;

        long numberOfReferences = (batchSize * 10) + 1;
        for (int i = 0; i < numberOfReferences; i++) {
            getNodeFactory().getOrCreateStudy(createRandomStudy());
        }

        AtomicLong counter = processStudies(batchSize, getNodeIdCollector());

        assertThat(counter.get(), Is.is(numberOfReferences));
    }

    private AtomicLong processStudies(long batchSize, NodeIdCollector nodeIdCollector) {
        AtomicLong counter = new AtomicLong();
        new NodeProcessorImpl(
                getGraphDb(),
                batchSize,
                StudyConstant.TITLE_IN_NAMESPACE,
                "*",
                "studies",
                nodeIdCollector
        ).process(node -> counter.incrementAndGet());
        return counter;
    }

    private StudyImpl createRandomStudy() throws MalformedDOIException {
        String randomString = UUID.randomUUID().toString();
        return new StudyImpl(randomString, DOI.create("10.123/" + randomString), randomString);
    }

}