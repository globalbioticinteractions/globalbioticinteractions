package org.eol.globi.process;

import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.util.InteractUtil;
import org.globalbioticinteractions.dataset.Dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InteractionListenerImpl implements InteractionListener {

    private final List<InteractionListener> processors;
    private final List<Map<String, String>> outbox = new ArrayList<>();
    private final List<Map<String, String>> inbox = new ArrayList<>();

    public InteractionListenerImpl(NodeFactory nodeFactory,
                                   GeoNamesService geoNamesService,
                                   ImportLogger logger,
                                   Dataset dataset) {
        InteractionListener queue = interaction -> {
            if (interaction != null) {
                outbox.add(interaction);
            }
        };
        try {
            InteractionListener mappingListener =
                    dataset == null
                            ? queue
                            : new InteractionListenerWithInteractionTypeMapping(queue, InteractUtil.createInteractionTypeMapperForImporter(dataset), logger);

            this.processors =
                    Arrays.asList(
                            new InteractionExpander(queue, logger),
                            mappingListener,
                            new InteractionValidator(queue, logger),
                            new InteractionImporter(nodeFactory, logger, geoNamesService)
                    );
        } catch (StudyImporterException e) {
            throw new IllegalArgumentException("failed to instantiate interaction processor for [" + dataset.getNamespace() + "]", e);
        }
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        outbox.add(interaction);
        try {
            for (InteractionListener processor : processors) {
                if (outbox.isEmpty()) {
                    break;
                } else {
                    inbox.addAll(outbox);
                    outbox.clear();
                }
                for (Map<String, String> incomingInteractions : inbox) {
                    processor.on(incomingInteractions);
                }
                inbox.clear();
            }
        } catch (StudyImporterException e) {
            throw new StudyImporterException("failed to import: " + interaction, e);
        }
    }

}
