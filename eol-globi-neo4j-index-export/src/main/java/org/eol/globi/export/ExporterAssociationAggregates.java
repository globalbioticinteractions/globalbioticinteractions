package org.eol.globi.export;

import org.eol.globi.domain.StudyNode;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExporterAssociationAggregates extends ExporterAssociationsBase {

    public ExporterAssociationAggregates(GraphDatabaseService graphService) {
        super(graphService);
    }

    @Override
    public void doExportStudy(StudyNode study, ExportUtil.Appender writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            ExporterAggregateUtil.exportDistinctInteractionsByStudy(
                    writer,
                    getGraphService(),
                    new AssociationWriter()
            );
        }
    }

    class AssociationWriter implements ExporterAggregateUtil.RowWriter {

        @Override
        public void writeRow(ExportUtil.Appender writer,
                             StudyNode study,
                             String sourceTaxonId,
                             String interactionType,
                             List<String> targetTaxonIds) throws IOException {
            Map<String, String> properties = new HashMap<String, String>();
            for (String targetTaxonId : targetTaxonIds) {
                String sourceOccurrenceId = study.getNodeID() + "-" + sourceTaxonId + "-" + interactionType;
                String assocIdAndTargetOccurrenceIdId = sourceOccurrenceId + "-" + targetTaxonId;
                properties.put(EOLDictionary.ASSOCIATION_ID, "globi:assoc:" + assocIdAndTargetOccurrenceIdId);
                properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:source:" + sourceOccurrenceId);
                properties.put(EOLDictionary.TARGET_OCCURRENCE_ID, "globi:occur:target:" + assocIdAndTargetOccurrenceIdId);
                properties.put(EOLDictionary.ASSOCIATION_TYPE, getEOLTermFor(interactionType));
                addStudyInfo(study, properties);
                writeProperties(writer, properties);
                properties.clear();
            }
        }
    }
}
