package org.globalbioticinteractions.dataset;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetRegistryProxy implements DatasetRegistry {

    private final static Log LOG = LogFactory.getLog(DatasetRegistryProxy.class);

    private final ArrayList<DatasetRegistry> registries;
    private Map<String, DatasetRegistry> registryForNamespace = null;
    ;

    public DatasetRegistryProxy(List<DatasetRegistry> registries) {
        this.registries = new ArrayList<DatasetRegistry>() {{
            addAll(registries);
        }};
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetRegistryException {
        Collection<String> namespacesAll = new ArrayList<>();
        for (DatasetRegistry registry : registries) {
            Collection<String> namespaces = registry.findNamespaces();
            Collection<String> newNamespaces = CollectionUtils.subtract(namespaces, namespacesAll);
            for (String newNamespace : newNamespaces) {
                LOG.info("associating [" + newNamespace + "] with [" + registry.getClass().getSimpleName() + "]");
                associateNamespaceWithRegistry(registry, newNamespace);
            }
            namespacesAll.addAll(newNamespaces);
        }

        return namespacesAll;
    }

    public void associateNamespaceWithRegistry(DatasetRegistry registry, String newNamespace) {
        if (registryForNamespace == null) {
            registryForNamespace = new HashMap<>();
        }
        registryForNamespace.put(newNamespace, registry);
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        DatasetRegistry registry = registryForNamespace == null
                ? null
                : registryForNamespace.get(namespace);

        Dataset dataset = registry == null
                ? queryForDataset(namespace)
                : registry.datasetFor(namespace);

        if (dataset == null) {
            throw new DatasetRegistryException("failed to find dataset for [" + namespace + "]");
        }

        return dataset;
    }

    private Dataset queryForDataset(String namespace) throws DatasetRegistryException {
        Dataset dataset = null;
        DatasetRegistryException lastException = null;
        for (DatasetRegistry datasetRegistry : registries) {
            try {
                dataset = datasetRegistry.datasetFor(namespace);
                if (dataset != null) {
                    associateNamespaceWithRegistry(datasetRegistry, namespace);
                    break;
                }
            } catch (DatasetRegistryException ex) {
                lastException = ex;
            }

        }
        if (dataset == null && lastException != null) {
            throw new DatasetRegistryException("failed to find dataset for [" + namespace + "] possibly due to unexpected error", lastException);
        }
        return dataset;
    }

}
