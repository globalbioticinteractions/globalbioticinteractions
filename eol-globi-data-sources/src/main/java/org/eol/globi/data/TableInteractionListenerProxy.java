package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.domain.InteractType;

import java.util.HashMap;
import java.util.Map;

public class TableInteractionListenerProxy implements InteractionListener {
    final InteractionListener interactionListener;
    private final String baseUrl;
    private final JsonNode config;

    public TableInteractionListenerProxy(String baseUrl, JsonNode config, InteractionListener interactionListener) {
        this.baseUrl = baseUrl;
        this.config = config;
        this.interactionListener = interactionListener;
    }

    @Override
    public void newLink(final Map<String, String> properties) throws StudyImporterException {
        final String dataSourceCitation = StudyImporterForMetaTable.generateSourceCitation(baseUrl, config);

        final HashMap<String, String> enrichedProperties = new HashMap<String, String>() {
            {
                putAll(properties);
                put(StudyImporterForTSV.STUDY_SOURCE_CITATION, dataSourceCitation);
                final String referenceCitation = StudyImporterForMetaTable.generateReferenceCitation(properties);
                put(StudyImporterForTSV.REFERENCE_ID, dataSourceCitation + referenceCitation);
                put(StudyImporterForTSV.REFERENCE_CITATION, StringUtils.isBlank(referenceCitation) ? dataSourceCitation : referenceCitation);

                if (!properties.containsKey(StudyImporterForTSV.SOURCE_TAXON_NAME)) {
                    put(StudyImporterForTSV.SOURCE_TAXON_NAME, StudyImporterForMetaTable.generateSourceTaxonName(properties));
                }
                if (!properties.containsKey(StudyImporterForTSV.TARGET_TAXON_NAME)) {
                    put(StudyImporterForTSV.TARGET_TAXON_NAME, StudyImporterForMetaTable.generateTargetTaxonName(properties));
                }
            }

        };
        InteractType type = StudyImporterForMetaTable.generateInteractionType(enrichedProperties);
        StudyImporterForMetaTable.setInteractionType(enrichedProperties, type);

        interactionListener.newLink(enrichedProperties);
    }
}
