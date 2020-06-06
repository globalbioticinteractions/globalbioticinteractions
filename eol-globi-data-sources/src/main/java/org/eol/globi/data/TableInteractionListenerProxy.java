package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.dataset.Dataset;

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
    public void newLink(final Map<String, String> link) throws StudyImporterException {
        final Map<String, String> enrichedProperties = new TreeMap<String, String>() {
            {
                putAll(link);
                put(StudyImporterForTSV.STUDY_SOURCE_CITATION, dataSourceCitation);
                final String referenceCitation = StringUtils.isBlank(link.get(StudyImporterForTSV.REFERENCE_CITATION))
                        ? StudyImporterForMetaTable.generateReferenceCitation(link)
                        : link.get(StudyImporterForTSV.REFERENCE_CITATION);

                putIfAbsent(StudyImporterForTSV.REFERENCE_ID, dataSourceCitation + referenceCitation);
                putIfAbsent(StudyImporterForTSV.REFERENCE_CITATION,
                        StringUtils.isBlank(referenceCitation)
                        ? dataSourceCitation :
                        referenceCitation);
            }
        };

        interactionListener.newLink(enrichedProperties);
    }

}
