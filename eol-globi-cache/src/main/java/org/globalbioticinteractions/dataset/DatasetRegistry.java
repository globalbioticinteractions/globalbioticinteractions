package org.globalbioticinteractions.dataset;

import java.util.function.Consumer;

public interface DatasetRegistry extends DatasetFactory {
    Iterable<String> findNamespaces() throws DatasetRegistryException;

    void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException;

}
