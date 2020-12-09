package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
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
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LinkerDOI implements IndexerNeo4j {

    private static final Logger LOG = LoggerFactory.getLogger(LinkerDOI.class);
    public static final int BATCH_SIZE = 25;

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
        Transaction transaction = graphDb.beginTx();
        try {
            Index<Node> taxons = graphDb.index().forNodes("studies");
            IndexHits<Node> hits = taxons.query("*:*");

            int counter = 0;
            int counterResolved = 0;
            String msg = "linking study citations to DOIs";
            LOG.info(msg + " started...");
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Map<String, StudyNode> batch = new HashMap<>();
            for (Node hit : hits) {
                counter++;
                StudyNode study = new StudyNode(hit);
                if (shouldResolve(study)) {
                    counterResolved++;
                    batch.put(study.getCitation(), study);
                }

                if (batch.size() >= BATCH_SIZE) {
                    LOG.info(logProgress(counterResolved, stopWatch));
                    resolveBatch(doiResolver, batch);
                    batch.clear();
                    transaction.success();
                    transaction.close();
                    transaction = graphDb.beginTx();
                }
            }
            resolveBatch(doiResolver, batch);

            LOG.info(msg + " complete. Out of [" + counter + "] references, [" + counterResolved + "] needed resolving.");
            if (counter % 100 != 0) {
                LOG.info(logProgress(counterResolved, stopWatch));
            }
            stopWatch.stop();
            transaction.success();
        } finally {
            transaction.close();
        }
    }

    public void resolveBatch(DOIResolver doiResolver, Map<String, StudyNode> batch) {
        try {
            resolve(doiResolver, batch, batch.keySet());
        } catch (IOException e) {
            LOG.info("failed to request DOIs by batch of [" + BATCH_SIZE + "], attempting to query one by one", e);
            Map<String, Exception> errMap = new HashMap<>();
            for (String citation : batch.keySet()) {
                try {
                    resolve(doiResolver, batch, Collections.singletonList(citation));
                } catch (IOException e1) {
                    errMap.put(citation, e1);
                }
            }
            for (String s : errMap.keySet()) {
                LOG.error("failed to retrieve DOI for [" + s + "]", errMap.get(s));
            }
        } finally {
            batch.clear();
        }
    }

    public void resolve(DOIResolver doiResolver, Map<String, StudyNode> batch, Collection<String> citations) throws IOException {
        Map<String, DOI> doiMap = doiResolver.resolveDoiFor(citations);
        for (String s : doiMap.keySet()) {
            StudyNode studyNode = batch.get(s);
            if (studyNode != null) {
                DOI doiResolved = doiMap.get(s);
                if (doiResolved != null) {
                    setDOIForStudy(studyNode, doiResolved);
                }
            }
        }
    }

    public String logProgress(int counter, StopWatch stopWatch) {
        stopWatch.suspend();
        String msg = "linked [%d] reference(s) in [%.1f] s  at rate of [%.1f] references/s)";
        stopWatch.resume();
        return String.format(msg, counter, stopWatch.getTime() / 1000.0, 1000.0 * counter / stopWatch.getTime());
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
            study.setPropertyWithTx(StudyConstant.DOI, doiResolved.toString());
            if (StringUtils.isBlank(study.getExternalId())) {
                study.setPropertyWithTx(PropertyAndValueDictionary.EXTERNAL_ID, doiResolved.toURI().toString());
            }
        }
    }

    public static boolean shouldResolve(Study study) {
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
