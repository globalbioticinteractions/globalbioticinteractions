package org.eol.globi.process;

import org.eol.globi.data.OccurrenceIdIdEnricherGenBank;
import org.eol.globi.data.OccurrenceIdIdEnricherINaturalist;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.LogUtil;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.OccurrenceIdEnricher;
import org.eol.globi.data.SpecimenCitationEnricher;
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

        this.processors =
                Arrays.asList(
                        new OccurrenceIdIdEnricherINaturalist(queue, logger, dataset),
                        new OccurrenceIdIdEnricherGenBank(queue, logger, dataset),
                        new OccurrenceIdEnricher(queue, logger),
                        new TaxonNameEnricher(queue, logger),
                        new InteractionExpander(queue, logger),
                        new SpecimenCitationEnricher(queue, logger),
                        createMappingListener(logger, dataset, queue),
                        new InteractionValidator(queue, logger),
                        new DOIReferenceExtractor(queue, logger),
                        new InteractionImporter(nodeFactory, logger, geoNamesService)
                );
    }

    public InteractionListener createMappingListener(ImportLogger logger, Dataset dataset, InteractionListener queue) {
        InteractionListener mappingListener = queue;

        try {
            mappingListener =
                    dataset == null
                            ? queue
                            : new InteractionListenerWithInteractionTypeMapping(queue, InteractUtil.createInteractionTypeMapperForImporter(dataset), logger);
        } catch (StudyImporterException ex) {
            LogUtil.logError(logger, "custom translation mapper not enabled, because the mapper for [" + dataset.getNamespace() + "] failed to initialize", ex);
        }
        return mappingListener;
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
