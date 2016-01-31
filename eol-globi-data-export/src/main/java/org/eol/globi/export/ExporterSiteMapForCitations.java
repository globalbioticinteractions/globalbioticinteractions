package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.export.SiteMapUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ExporterSiteMapForCitations implements StudyExporter {

    private final File baseDir;

    public ExporterSiteMapForCitations(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        Set<String> accordingToHits = new HashSet<String>();
        if (includeHeader) {
            // just do it once
            final List<Study> allStudies = NodeUtil.findAllStudies(study.getUnderlyingNode().getGraphDatabase());
            for (Study allStudy : allStudies) {
                final String doi = allStudy.getDOI();
                if (StringUtils.isNotBlank(doi)) {
                    accordingToHits.add(doi);
                } else if (StringUtils.isNotBlank(allStudy.getCitation())) {
                    accordingToHits.add(allStudy.getCitation());
                }
                accordingToHits.add(allStudy.getSource());
            }
        }
        SiteMapUtils.generateSiteMapFor("accordingTo=", accordingToHits, baseDir);
    }
}
