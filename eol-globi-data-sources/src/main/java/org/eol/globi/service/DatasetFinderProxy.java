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

public class DatasetFinderProxy implements DatasetFinder {

    private final static Log LOG = LogFactory.getLog(DatasetFinderProxy.class);

    private final ArrayList<DatasetFinder> finders;
    private Map<String, DatasetFinder> finderForNamespace = null;
    private Collection<String> namespacesAll;

    public DatasetFinderProxy(List<DatasetFinder> finders) {
        this.finders = new ArrayList<DatasetFinder>() {{
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
        DatasetFinder finder = finderForNamespace.get(namespace);
        if (finder == null) {
            throw new DatasetFinderException("unknown namespace [" + namespace + "]");
        }
        return finder.datasetFor(namespace);
    }

    private void lazyInit() throws DatasetFinderException {
        if (namespacesAll == null || finderForNamespace == null) {
            namespacesAll = new HashSet<>();
            finderForNamespace = new HashMap<>();
            for (DatasetFinder finder : finders) {
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
