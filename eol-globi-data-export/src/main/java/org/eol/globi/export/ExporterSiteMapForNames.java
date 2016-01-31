package org.eol.globi.export;

import org.eol.globi.data.export.SiteMapUtils;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ExporterSiteMapForNames implements StudyExporter {

    private final File baseDir;

    public ExporterSiteMapForNames(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        Set<String> names = new HashSet<String>();
        if (includeHeader) {
            // just do it once
            final List<Study> allStudies = NodeUtil.findAllStudies(study.getUnderlyingNode().getGraphDatabase());
            for (Study allStudy : allStudies) {
                final Iterable<Relationship> specimens = allStudy.getSpecimens();
                for (Relationship specimen : specimens) {
                    final Iterable<Relationship> relationships = specimen.getEndNode().getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
                    if (relationships.iterator().hasNext()) {
                        final TaxonNode taxonNode = new TaxonNode(relationships.iterator().next().getEndNode());
                        names.add(taxonNode.getName());
                    }
                }
            }
        }
        SiteMapUtils.generateSiteMapFor("interactionType=interactsWith&sourceTaxonName=", names, baseDir);
    }
}
