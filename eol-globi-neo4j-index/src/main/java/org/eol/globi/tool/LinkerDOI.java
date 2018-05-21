package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.util.DOIUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LinkerDOI implements Linker {

    private static final Log LOG = LogFactory.getLog(LinkerDOI.class);
    public static final int BATCH_SIZE = 25;

    private final DOIResolver doiResolver;
    private final GraphDatabaseService graphDb;

    public LinkerDOI(GraphDatabaseService graphDb) {
        this(graphDb, new DOIResolverImpl());
    }

    public LinkerDOI(GraphDatabaseService graphDb, DOIResolver resolver) {
        this.graphDb = graphDb;
        this.doiResolver = resolver;
    }

    @Override
    public void link() {
        Index<Node> taxons = this.graphDb.index().forNodes("studies");
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
            }
        }
        resolveBatch(doiResolver, batch);

        LOG.info(msg + " complete. Out of [" + counter + "] references, [" + counterResolved + "] needed resolving.");
        if (counter % 100 != 0) {
            LOG.info(logProgress(counterResolved, stopWatch));
        }
        stopWatch.stop();
    }

    public void resolveBatch(DOIResolver doiResolver, Map<String, StudyNode> batch) {
        try {
            resolve(doiResolver, batch, batch.keySet());
        } catch (IOException e) {
            LOG.info("failed to request DOIs by batch of [" + BATCH_SIZE + "], attempting to resolve one by one", e);
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
        Map<String, String> doiMap = doiResolver.resolveDoiFor(citations);
        for (String s : doiMap.keySet()) {
            StudyNode studyNode = batch.get(s);
            if (studyNode != null) {
                String doiResolved = doiMap.get(s);
                if (StringUtils.isNotBlank(doiResolved)) {
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


    public void linkStudy(DOIResolver doiResolver, StudyNode study) {
        if (shouldResolve(study)) {
            try {
                String doiResolved = doiResolver.resolveDoiFor(study.getCitation());
                setDOIForStudy(study, doiResolved);
            } catch (IOException e) {
                LOG.warn("failed to lookup doi for citation [" + study.getCitation() + "] with id [" + study.getTitle() + "]", e);
            }
        }
    }

    private void setDOIForStudy(StudyNode study, String doiResolved) {
        if (StringUtils.isNotBlank(doiResolved)) {
            String doiUrl = DOIUtil.urlForDOI(doiResolved);
            study.setPropertyWithTx(StudyConstant.DOI, doiUrl);
            if (StringUtils.isBlank(study.getExternalId())) {
                study.setPropertyWithTx(PropertyAndValueDictionary.EXTERNAL_ID, doiUrl);
            }
        }
    }

    public static boolean shouldResolve(Study study) {
        Dataset dataset = study.getOriginatingDataset();
        return DatasetUtil.shouldResolveReferences(dataset)
                && StringUtils.isBlank(study.getDOI())
                && StringUtils.isNotBlank(study.getCitation())
                && citationLikeString(study.getCitation());
    }

    private static boolean citationLikeString(String citation) {
        return !StringUtils.startsWith(citation, "http://");
    }


}
