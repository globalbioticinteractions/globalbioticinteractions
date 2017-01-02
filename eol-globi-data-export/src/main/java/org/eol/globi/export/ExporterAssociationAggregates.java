package org.eol.globi.export;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExporterAssociationAggregates extends ExporterAssociationsBase {

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            ExporterAggregateUtil.exportDistinctInteractionsByStudy(writer, ((NodeBacked)study).getUnderlyingNode().getGraphDatabase(), new AssociationWriter());
        }
    }

    class AssociationWriter implements ExporterAggregateUtil.RowWriter {

        @Override
        public void writeRow(Writer writer, StudyNode study, String sourceTaxonId, String interactionType, List<String> targetTaxonIds) throws IOException {
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
