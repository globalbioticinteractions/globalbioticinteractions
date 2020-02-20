package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.InteractTypeMapperFactory;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.dataset.Dataset;

import java.util.HashMap;
import java.util.Map;

public class TableInteractionListenerProxy implements InteractionListener {
    private final InteractionListener interactionListener;
    private final Dataset dataset;
    private final String dataSourceCitation;
    private InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper;

    public TableInteractionListenerProxy(Dataset dataset, InteractionListener interactionListener) {
        this.dataset = dataset;
        this.interactionListener = interactionListener;
        this.dataSourceCitation = CitationUtil.sourceCitationLastAccessed(dataset);
    }

    private InteractTypeMapperFactory.InteractTypeMapper getInteractionTypeMapper() throws TermLookupServiceException {
        if (interactTypeMapper == null) {
            interactTypeMapper = new InteractTypeMapperFactoryImpl(dataset).create();
        }
        return interactTypeMapper;
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
            }
        };

        TaxonUtil.enrichTaxonNames(enrichedProperties);
        InteractType type;
        try {
            type = StudyImporterForMetaTable.generateInteractionType(enrichedProperties, getInteractionTypeMapper());
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException("failed to map interaction types", e);
        }
        StudyImporterForMetaTable.setInteractionType(enrichedProperties, type);
        interactionListener.newLink(enrichedProperties);
    }

}
