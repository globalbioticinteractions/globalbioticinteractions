package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.eol.globi.util.InteractUtil;

import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID_VERBATIM;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_NAME_VERBATIM;

public class InteractionListenerWithInteractionTypeMapping implements InteractionListener {
    private final InteractTypeMapper mapper;
    final InteractionListener listener;
    private final ImportLogger logger;

    InteractionListenerWithInteractionTypeMapping(InteractionListener listener,
                                                  InteractTypeMapper mapper,
                                                  ImportLogger logger) {
        this.listener = listener;
        this.mapper = mapper;
        this.logger = logger;
    }

    @Override
    public void newLink(Map<String, String> link) throws StudyImporterException {
        TaxonUtil.enrichTaxonNames(link);

        String interactionTypeName = link.get(INTERACTION_TYPE_NAME);
        if (mapper.shouldIgnoreInteractionType(interactionTypeName)) {
            if (logger != null) {
                logger.info(LogUtil.contextFor(link), "ignoring interaction record for name [" + interactionTypeName + "]");
            }
        } else {
            InteractType mappedType = null;
            if (link.containsKey(INTERACTION_TYPE_ID)) {
                mappedType = mapper.getInteractType(link.get(INTERACTION_TYPE_ID));
            }

            if (mappedType == null && link.containsKey(INTERACTION_TYPE_NAME)) {
                mappedType = mapper.getInteractType(interactionTypeName);
            }

            HashMap<String, String> properties = new HashMap<>(link);
            if (mappedType != null) {
                properties.put(INTERACTION_TYPE_ID, mappedType.getIRI());
                properties.put(INTERACTION_TYPE_NAME, mappedType.getLabel());
                InteractUtil.putNotBlank(properties, INTERACTION_TYPE_ID_VERBATIM, properties.get(INTERACTION_TYPE_ID));
                InteractUtil.putNotBlank(properties, INTERACTION_TYPE_NAME_VERBATIM, properties.get(INTERACTION_TYPE_NAME));
            }
            listener.newLink(properties);
        }


    }


}
