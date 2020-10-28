package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DatasetRegistryFiltered implements DatasetRegistry {

    private final DatasetRegistry registry;
    private final Predicate<String> predicate;

    public DatasetRegistryFiltered(Predicate<String> predicate, DatasetRegistry registry) {
        this.predicate = predicate;
        this.registry = registry;
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetRegistryException {
        return registry.findNamespaces()
                .stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        return registry.datasetFor(namespace);
    }
}
