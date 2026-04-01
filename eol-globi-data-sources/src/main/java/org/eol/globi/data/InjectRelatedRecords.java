package org.eol.globi.data;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.process.InteractionListener;
import org.globalbioticinteractions.dataset.Dataset;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InjectRelatedRecords implements InteractionListener {

    private final Map<String, Map<String, Map<String, String>>> indexedDependencies;
    private final InteractionListener listener;
    private final ImportLogger logger;
    private final Dataset dataset;
    private final Map<String, String> injectionMap;

    public InjectRelatedRecords(InteractionListener listener,
                                Dataset dataset,
                                Map<String, Map<String, Map<String, String>>> indexedDependencies,
                                ImportLogger logger) {
        this.listener = listener;
        this.indexedDependencies = indexedDependencies;
        Set<String> a = indexedDependencies.keySet();
        Stream<Pair<String, String>> expandedKeys = Stream.concat(Stream.concat(indexedDependencies.keySet().stream().map(n -> Pair.of(n, n)),
                a.stream().map(name -> Pair.of(name, prefixWithSource(name)))),
                a.stream().map(name -> Pair.of(name, prefixWithTarget(name))));

        this.injectionMap = expandedKeys.collect(Collectors.toMap(Pair::getValue, Pair::getKey));

        this.logger = logger;
        this.dataset = dataset;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        Map<String, String> handledInteraction = interaction;
        if (indexedDependencies != null && indexedDependencies.size() > 0) {

            Collection<String> propertiesToInjected
                    = CollectionUtils.intersection(injectionMap.keySet(), interaction.keySet());

            if (!propertiesToInjected.isEmpty()) {
                handledInteraction = injectPropertyValues(
                        interaction,
                        propertiesToInjected
                );
            }
        }
        listener.on(handledInteraction);
    }

    private Map<String, String> injectPropertyValues(Map<String, String> interaction, Collection<String> propertiesToInjected) {

        Map<String, String> handledInteraction = new TreeMap<>(interaction);
        for (String propertyToBeInjected : propertiesToInjected) {
            injectValuesByProperty(interaction, propertyToBeInjected, handledInteraction);
        }

        Collection<String> remainingPropertiesToBeInjected
                = CollectionUtils.disjunction(
                CollectionUtils.intersection(injectionMap.keySet(), handledInteraction.keySet()),
                CollectionUtils.intersection(injectionMap.keySet(), interaction.keySet())
        );

        if (!remainingPropertiesToBeInjected.isEmpty()) {
            handledInteraction = injectPropertyValues(
                    handledInteraction,
                    remainingPropertiesToBeInjected
            );
        }
        return handledInteraction;
    }

    private void injectValuesByProperty(Map<String, String> interaction, String propertyToBeInjected, Map<String, String> handledInteraction) {
        String indexedPropertyToBeInjected = injectionMap.get(propertyToBeInjected);
        String keyToBeExpanded = interaction.get(propertyToBeInjected);
        if (StringUtils.isNotBlank(keyToBeExpanded)) {
            final Map<String, String> injectable = indexedDependencies
                    .get(indexedPropertyToBeInjected)
                    .get(keyToBeExpanded);
            if (injectable == null) {
                logger.warn(LogUtil.contextFor(interaction), "no matching values for foreign key [" + propertyToBeInjected + ":" + keyToBeExpanded + "] found.");
            } else {
                if (!indexedDependencies.containsKey(propertyToBeInjected) && StringUtils.startsWith(propertyToBeInjected, getSourceLabel())) {
                    for (String key : injectable.keySet()) {
                        String value = injectable.get(key);
                        if (StringUtils.isNotBlank(value)) {
                            handledInteraction.put(prefixWithSource(key), value);
                        }
                    }
                } else if (!indexedDependencies.containsKey(propertyToBeInjected) && StringUtils.startsWith(propertyToBeInjected, getTargetLabel())) {
                    for (String key : injectable.keySet()) {
                        String value = injectable.get(key);
                        if (StringUtils.isNotBlank(value)) {
                            handledInteraction.put(prefixWithTarget(key), value);
                        }
                    }
                } else {
                    handledInteraction.putAll(injectable);
                }

            }
        }
    }


    private String prefixWithSource(String name) {
        return getSourceLabel() + StringUtils.capitalize(name);
    }

    private String prefixWithTarget(String name) {
        return getTargetLabel() + StringUtils.capitalize(name);
    }

    private String getSourceLabel() {
        return "source";
    }

    private String getTargetLabel() {
        return "target";
    }


}
