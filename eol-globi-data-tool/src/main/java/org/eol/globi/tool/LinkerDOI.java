package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NamedNode;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetUtil;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;

public class LinkerDOI {

    private static final Log LOG = LogFactory.getLog(LinkerDOI.class);

    public void link(final GraphDatabaseService graphDb) {
        Index<Node> taxons = graphDb.index().forNodes("studies");
        IndexHits<Node> hits = taxons.query("*:*");

        DOIResolver doiResolver = new DOIResolverImpl();
        int counter = 0;
        String msg = "linking study citations to DOIs";
        LOG.info(msg + " started...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (Node hit : hits) {
            counter++;
            if (counter % 100 == 0) {
                LOG.info(logProgress(counter, stopWatch));
            }
            linkStudy(doiResolver, new StudyNode(hit));
        }
        LOG.info(msg + " complete.");
        if (counter % 100 != 0) {
            LOG.info(logProgress(counter, stopWatch));
        }
        stopWatch.stop();
    }

    public String logProgress(int counter, StopWatch stopWatch) {
        stopWatch.suspend();
        String msg = "linked [%d] reference(s) in [%d] ms  at rate of [%.1f] references/s)";
        stopWatch.resume();
        return String.format(msg, counter, stopWatch.getTime(), 1000.0 * counter / stopWatch.getTime());
    }

    public void linkStudy(DOIResolver doiResolver, StudyNode study) {
        Dataset dataset = study.getOriginatingDataset();
        if (DatasetUtil.shouldResolveReferences(dataset)) {
            try {
                String doiResolved = study.getDOI();
                if (StringUtils.isBlank(study.getDOI()) && citationLikeString(study.getCitation())) {
                    doiResolved = doiResolver.findDOIForReference(study.getCitation());
                }

                if (StringUtils.isNotBlank(doiResolved)) {
                    study.setPropertyWithTx(StudyConstant.DOI, doiResolved);
                    if (StringUtils.isBlank(study.getExternalId())) {
                        study.setPropertyWithTx(PropertyAndValueDictionary.EXTERNAL_ID, ExternalIdUtil.urlForExternalId(doiResolved));
                    }
                }
            } catch (IOException e) {
                LOG.warn("failed to lookup doi for citation [" + study.getCitation() + "] with id [" + study.getTitle() + "]", e);
            }
        }
    }

    private boolean citationLikeString(String citation) {
        return !StringUtils.startsWith(citation, "http://");
    }


}
