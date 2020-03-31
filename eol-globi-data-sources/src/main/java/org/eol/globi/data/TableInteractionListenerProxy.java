package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.InteractTypeMapperFactory;
import org.eol.globi.util.InteractTypeMapperFactoryForRO;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.eol.globi.util.InteractTypeMapperFactoryWithFallback;
import org.eol.globi.util.InteractUtil;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.dataset.Dataset;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TableInteractionListenerProxy implements InteractionListener {
    private final InteractionListener interactionListener;
    private final String dataSourceCitation;

    public TableInteractionListenerProxy(Dataset dataset, InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
        this.dataSourceCitation = CitationUtil.sourceCitationLastAccessed(dataset);
    }

    @Override
    public void newLink(final Map<String, String> properties) throws StudyImporterException {
        final Map<String, String> enrichedProperties = new TreeMap<String, String>() {
            {
                putAll(properties);
                put(StudyImporterForTSV.STUDY_SOURCE_CITATION, dataSourceCitation);
                final String referenceCitation = StringUtils.isBlank(properties.get(StudyImporterForTSV.REFERENCE_CITATION)) ? StudyImporterForMetaTable.generateReferenceCitation(properties) : properties.get(StudyImporterForTSV.REFERENCE_CITATION);
                put(StudyImporterForTSV.REFERENCE_ID, dataSourceCitation + referenceCitation);
                put(StudyImporterForTSV.REFERENCE_CITATION, StringUtils.isBlank(referenceCitation) ? dataSourceCitation : referenceCitation);
            }
        };

        interactionListener.newLink(enrichedProperties);
    }

}
