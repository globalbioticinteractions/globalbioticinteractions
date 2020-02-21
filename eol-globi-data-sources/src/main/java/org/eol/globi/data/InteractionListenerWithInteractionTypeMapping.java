package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
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
        String interactionTypeId = link.get(INTERACTION_TYPE_ID);
        if (StringUtils.isBlank(interactionTypeName) && StringUtils.isBlank(interactionTypeId)) {
            if (logger != null) {
                logger.info(LogUtil.contextFor(link), "no interaction type defined");
            }
        } else if (mapper.shouldIgnoreInteractionType(interactionTypeName)) {
            if (logger != null) {
                logger.info(LogUtil.contextFor(link), "ignoring interaction record for name [" + interactionTypeName + "]");
            }
        } else {
            InteractType mappedType = null;
            if (StringUtils.isNotBlank(interactionTypeId)) {
                mappedType = mapper.getInteractType(interactionTypeId);
            }

            if (mappedType == null && StringUtils.isNotBlank(interactionTypeName)) {
                mappedType = mapper.getInteractType(interactionTypeName);
            }

            HashMap<String, String> properties = new HashMap<>(link);
            if (mappedType != null) {
                InteractUtil.putNotBlank(properties, INTERACTION_TYPE_ID_VERBATIM, properties.get(INTERACTION_TYPE_ID));
                InteractUtil.putNotBlank(properties, INTERACTION_TYPE_NAME_VERBATIM, properties.get(INTERACTION_TYPE_NAME));
                properties.put(INTERACTION_TYPE_ID, mappedType.getIRI());
                properties.put(INTERACTION_TYPE_NAME, mappedType.getLabel());
            }
            listener.newLink(properties);
        }


    }


}
