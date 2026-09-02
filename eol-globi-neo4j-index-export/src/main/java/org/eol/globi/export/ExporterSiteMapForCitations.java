package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.export.SiteMapUtils;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

class ExporterSiteMapForCitations implements GraphExporter {

    @Override
    public void export(GraphDatabaseService graphDb, File baseDir) throws StudyImporterException {
        Set<String> accordingToHits = new HashSet<String>();
        accordingToHits.add("gomexsi");

        NodeUtil.findStudies(
                graphDb,
                node -> {
                    final String doi = new StudyNode(node).getExternalId();
                    if (StringUtils.isNotBlank(doi)) {
                        accordingToHits.add(doi);
                    }
                });

        final String siteMapLocation = "https://depot.globalbioticinteractions.org/snapshot/target/data/sitemap/citations/";
        SiteMapUtils.generateSiteMap(accordingToHits, baseDir, "accordingTo=", siteMapLocation);
    }


}
