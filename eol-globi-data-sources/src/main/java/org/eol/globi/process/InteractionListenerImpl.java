package org.eol.globi.process;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.LogUtil;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.OccurrenceIdEnricherAtlasOfLivingAustralia;
import org.eol.globi.data.OccurrenceIdEnricherCaliforniaAcademyOfSciences;
import org.eol.globi.data.OccurrenceIdEnricherFieldMuseum;
import org.eol.globi.data.OccurrenceIdIdEnricherGenBank;
import org.eol.globi.data.OccurrenceIdIdEnricherINaturalist;
import org.eol.globi.data.SpecimenCitationEnricher;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.InteractUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        String shouldResolveReferences
                = dataset == null ? "false" : dataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "false");


        List<InteractionListener> resolvingEnrichers = Arrays.asList(
                new OccurrenceIdIdEnricherINaturalist(queue, logger, dataset),
                new OccurrenceIdIdEnricherGenBank(queue, logger, dataset)
        );

        List<InteractionListener> listeners = new ArrayList<>();

        if (StringUtils.equalsIgnoreCase(shouldResolveReferences, "true")) {
            listeners.addAll(resolvingEnrichers);
        }

        listeners.addAll(
                Arrays.asList(
                        new OccurrenceIdEnricherFieldMuseum(queue, logger),
                        new OccurrenceIdEnricherCaliforniaAcademyOfSciences(queue, logger),
                        new OccurrenceIdEnricherAtlasOfLivingAustralia(queue, logger),
                        new TaxonNameEnricher(queue, logger),
                        new InteractionExpander(queue, logger),
                        new SpecimenCitationEnricher(queue, logger),
                        createMappingListener(logger, dataset, queue),
                        new InteractionValidator(queue, logger),
                        new DOIReferenceExtractor(queue, logger),
                        new InteractionImporter(nodeFactory, logger, geoNamesService)
                ));

        this.processors = Collections.unmodifiableList(listeners);
    }

    public InteractionListener createMappingListener(ImportLogger logger, Dataset dataset, InteractionListener queue) {
        InteractionListener mappingListener = queue;

        try {
            mappingListener =
                    dataset == null
                            ? queue
                            : new InteractionListenerWithInteractionTypeMapping(queue, InteractUtil.createInteractionTypeMapperForImporter(dataset), logger);
        } catch (TermLookupServiceException ex) {
            LogUtil.logError(logger, "type mapper for [" + dataset.getNamespace() + "] disabled:  [" + ex.getMessage() + "]", ex);
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
