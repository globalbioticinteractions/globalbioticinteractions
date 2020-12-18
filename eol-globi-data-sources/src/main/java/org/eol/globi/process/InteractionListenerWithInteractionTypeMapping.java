package org.eol.globi.process;

import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.LogUtil;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.eol.globi.util.InteractUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID_VERBATIM;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME_VERBATIM;

public class InteractionListenerWithInteractionTypeMapping implements InteractionListener {
    private final InteractTypeMapper mapper;
    private final InteractionListener listener;
    private final ImportLogger logger;

    public InteractionListenerWithInteractionTypeMapping(InteractionListener listener,
                                                  InteractTypeMapper mapper,
                                                  ImportLogger logger) {
        this.listener = listener;
        this.mapper = mapper;
        this.logger = logger;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        TaxonUtil.enrichTaxonNames(interaction);

        String interactionTypeName = interaction.get(INTERACTION_TYPE_NAME);
        String interactionTypeId = interaction.get(INTERACTION_TYPE_ID);
        if (mapper.shouldIgnoreInteractionType(interactionTypeName) || mapper.shouldIgnoreInteractionType(interactionTypeId)) {
            if (logger != null) {
                logger.info(LogUtil.contextFor(interaction), "ignoring interaction record with interaction name [" + interactionTypeName + "]");
            }
        } else if (mapper.shouldIgnoreInteractionType(interactionTypeId)) {
            if (logger != null) {
                logger.info(LogUtil.contextFor(interaction), "ignoring interaction record with interaction id [" + interactionTypeName + "]");
            }
        } else {
            InteractType mappedType = null;
            if (interactionTypeId != null) {
                mappedType = mapper.getInteractType(interactionTypeId);
            }

            if (mappedType == null && interactionTypeName != null) {
                mappedType = mapper.getInteractType(interactionTypeName);
            }

            Map<String, String> properties = new TreeMap<>(interaction);
            if (mappedType != null) {
                InteractUtil.putNotNull(properties, INTERACTION_TYPE_ID_VERBATIM, properties.get(INTERACTION_TYPE_ID));
                InteractUtil.putNotNull(properties, INTERACTION_TYPE_NAME_VERBATIM, properties.get(INTERACTION_TYPE_NAME));
                properties.put(INTERACTION_TYPE_ID, mappedType.getIRI());
                properties.put(INTERACTION_TYPE_NAME, mappedType.getLabel());
            }
            listener.on(properties);
        }
    }

}
