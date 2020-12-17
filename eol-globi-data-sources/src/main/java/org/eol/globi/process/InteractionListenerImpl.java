package org.eol.globi.process;

import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.GeoNamesService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InteractionListenerImpl implements InteractionListener {

    private final List<InteractionListener> processors;
    private final InteractionListener queue;
    private List<Map<String, String>> interactions = new ArrayList<>();

    public InteractionListenerImpl(NodeFactory nodeFactory, GeoNamesService geoNamesService, ImportLogger logger) {
        this.queue = interaction -> {
            if (interaction != null) {
                interactions.add(interaction);
            }
        };
        this.processors =
                Arrays.asList(
                        new InteractionExpander(queue, logger),
                        new InteractionValidator(queue, logger),
                        new InteractionImporter(nodeFactory, logger, geoNamesService)
                );
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        queue.on(interaction);
        try {
            for (InteractionListener processor : processors) {
                if (interactions.isEmpty()) {
                    break;
                }
                ArrayList<Map<String, String>> incoming = new ArrayList<>(interactions);
                interactions.clear();
                for (Map<String, String> incomingInteractions : incoming) {
                    processor.on(incomingInteractions);
                }
            }
        } catch (StudyImporterException e) {
            throw new StudyImporterException("failed to import: " + interaction, e);
        }
    }

}
