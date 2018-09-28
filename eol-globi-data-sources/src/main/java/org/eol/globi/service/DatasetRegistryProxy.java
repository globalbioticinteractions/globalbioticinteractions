package org.eol.globi.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DatasetRegistryProxy implements DatasetRegistry {

    private final static Log LOG = LogFactory.getLog(DatasetRegistryProxy.class);

    private final ArrayList<DatasetRegistry> finders;
    private Map<String, DatasetRegistry> finderForNamespace = null;
    private Collection<String> namespacesAll;

    public DatasetRegistryProxy(List<DatasetRegistry> finders) {
        this.finders = new ArrayList<DatasetRegistry>() {{
            addAll(finders);
        }};
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetFinderException {
        lazyInit();
        return namespacesAll;
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        lazyInit();
        DatasetRegistry finder = finderForNamespace.get(namespace);
        if (finder == null) {
            throw new DatasetFinderException("unknown namespace [" + namespace + "]");
        }
        return finder.datasetFor(namespace);
    }

    private void lazyInit() throws DatasetFinderException {
        if (namespacesAll == null || finderForNamespace == null) {
            namespacesAll = new HashSet<>();
            finderForNamespace = new HashMap<>();
            for (DatasetRegistry finder : finders) {
                Collection<String> namespaces = finder.findNamespaces();
                Collection<String> newNamespaces = CollectionUtils.subtract(namespaces, namespacesAll);
                for (String newNamespace : newNamespaces) {
                    String msg = "associating [" + newNamespace + "] with [" + finder.getClass().getSimpleName() + "]";
                    LOG.info(msg);
                    finderForNamespace.put(newNamespace, finder);
                }
                namespacesAll.addAll(namespaces);
            }
        }
    }

}
