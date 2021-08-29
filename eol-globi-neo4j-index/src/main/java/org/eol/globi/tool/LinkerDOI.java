package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.util.BatchListener;
import org.eol.globi.util.NodeListener;
import org.eol.globi.util.NodeProcessorImpl;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.DOIResolverImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetUtil;
import org.globalbioticinteractions.doi.DOI;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class LinkerDOI implements IndexerNeo4j {

    private static final Logger LOG = LoggerFactory.getLogger(LinkerDOI.class);
    public static final long BATCH_SIZE = 25;

    private final DOIResolver doiResolver;

    public LinkerDOI() {
        this(new DOIResolverImpl());
    }

    public LinkerDOI(DOIResolver resolver) {
        this.doiResolver = resolver;
    }

    @Override
    public void index(GraphServiceFactory factory) {
        GraphDatabaseService graphDb = factory.getGraphService();

        final AtomicLong counter = new AtomicLong(0);
        final AtomicLong counterResolved = new AtomicLong(0);
        String msg = "linking study citations to DOIs";
        LOG.info(msg + " started...");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        new NodeProcessorImpl(graphDb,
                BATCH_SIZE,
                "*", "*", "studies")
                .process(node -> {
                    counter.incrementAndGet();
                    linkStudy(doiResolver, new StudyNode(node));
                }, new BatchListener() {
                    Transaction tx;
                    @Override
                    public void onStartBatch() {
                        if (tx != null) {
                            tx.success();
                            tx.close();
                        }
                        tx = graphDb.beginTx();
                    }

                    @Override
                    public void onFinishBatch() {
                        tx.success();
                        tx.close();
                    }
                });

        LOG.info(msg + " complete. Out of [" + counter.get() + "] references, [" + counterResolved.get() + "] needed resolving.");
        stopWatch.stop();


    }


    public static void linkStudy(DOIResolver doiResolver, StudyNode study) {
        if (shouldResolve(study)) {
            try {
                DOI doiResolved = doiResolver.resolveDoiFor(study.getCitation());
                setDOIForStudy(study, doiResolved);
            } catch (IOException e) {
                LOG.warn("failed to lookup doi for citation [" + study.getCitation() + "] with id [" + study.getTitle() + "]", e);
            }
        }
    }

    private static void setDOIForStudy(StudyNode study, DOI doiResolved) {
        if (null != doiResolved) {
            study.setProperty(StudyConstant.DOI, doiResolved.toString());
            if (StringUtils.isBlank(study.getExternalId())) {
                study.setProperty(PropertyAndValueDictionary.EXTERNAL_ID, doiResolved.toURI().toString());
            }
        }
    }

    static boolean shouldResolve(Study study) {
        Dataset dataset = study.getOriginatingDataset();
        return DatasetUtil.shouldResolveReferences(dataset)
                && null == study.getDOI()
                && StringUtils.isNotBlank(study.getCitation())
                && citationLikeString(study.getCitation());
    }

    private static boolean citationLikeString(String citation) {
        return !StringUtils.startsWith(citation, "http://");
    }


}
