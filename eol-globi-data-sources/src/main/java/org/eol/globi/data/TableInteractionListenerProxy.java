package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.util.HashMap;
import java.util.Map;

public class TableInteractionListenerProxy implements InteractionListener {
    private final InteractionListener interactionListener;
    private final Dataset dataset;
    private final String dataSourceCitation;

    public TableInteractionListenerProxy(Dataset dataset, InteractionListener interactionListener) {
        this.dataset = dataset;
        this.interactionListener = interactionListener;
        this.dataSourceCitation = CitationUtil.sourceCitationLastAccessed(dataset);
    }

    @Override
    public void newLink(final Map<String, String> properties) throws StudyImporterException {
        final HashMap<String, String> enrichedProperties = new HashMap<String, String>() {
            {
                putAll(properties);
                put(StudyImporterForTSV.STUDY_SOURCE_CITATION, dataSourceCitation);
                final String referenceCitation = StringUtils.isBlank(properties.get(StudyImporterForTSV.REFERENCE_CITATION)) ? StudyImporterForMetaTable.generateReferenceCitation(properties) : properties.get(StudyImporterForTSV.REFERENCE_CITATION);
                put(StudyImporterForTSV.REFERENCE_ID, dataSourceCitation + referenceCitation);
                put(StudyImporterForTSV.REFERENCE_CITATION, StringUtils.isBlank(referenceCitation) ? dataSourceCitation : referenceCitation);
                TaxonUtil.enrichTaxonNames(properties);
            }
        };

        InteractType type = StudyImporterForMetaTable.generateInteractionType(enrichedProperties);
        StudyImporterForMetaTable.setInteractionType(enrichedProperties, type);
        interactionListener.newLink(enrichedProperties);
    }

}
