package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NodeBacked;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxonPropertyEnricherImpl implements TaxonPropertyEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonPropertyEnricherImpl.class);

    private final List<TaxonPropertyLookupService> services = new ArrayList<TaxonPropertyLookupService>();
    private final HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();

    public static final Map<String, String> PROPERTIES = new HashMap<String, String>() {{
        put(NodeBacked.EXTERNAL_ID, null);
        put(Taxon.PATH, null);
    }};

    private final GraphDatabaseService graphDbService;

    public TaxonPropertyEnricherImpl(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
    }

    @Override
    public boolean enrich(Taxon taxon) throws IOException {
        boolean didEnrichAtLeastOneProperty = false;
        if (needsEnriching(taxon)) {
            didEnrichAtLeastOneProperty = doEnrichment(taxon, didEnrichAtLeastOneProperty);
        }
        return didEnrichAtLeastOneProperty;
    }

    private boolean needsEnriching(Taxon taxon) {
        boolean needsEnriching = false;
        if (StringUtils.isNotBlank(taxon.getName()) && taxon.getName().length() > 1) {
            Node underlyingNode = taxon.getUnderlyingNode();
            for (String propertyName : PROPERTIES.keySet()) {
                needsEnriching = needsEnriching || !underlyingNode.hasProperty(propertyName);
            }
        }
        return needsEnriching;
    }

    private boolean doEnrichment(Taxon taxon, boolean didEnrichAtLeastOneProperty) {
        Node taxonNode = taxon.getUnderlyingNode();

        Map<String, String> properties = new HashMap<String, String>(PROPERTIES);
        for (TaxonPropertyLookupService service : services) {
            try {
                if (enrichTaxonWithPropertyValue(errorCounts, taxonNode, service, properties)) {
                    didEnrichAtLeastOneProperty = true;
                    break;
                }
            } catch (TaxonPropertyLookupServiceException e) {
                LOG.warn("problem with taxon lookup", e);
                service.shutdown();
            }
        }

        return didEnrichAtLeastOneProperty;
    }

    private boolean enrichTaxonWithPropertyValue(HashMap<Class, Integer> errorCounts, Node
            taxonNode, TaxonPropertyLookupService service, Map<String, String> properties) throws
            TaxonPropertyLookupServiceException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
        } else {
            if (enrichTaxon(errorCounts, taxonNode, service, errorCount, properties)) {
                return true;
            }
        }
        return false;
    }

    private boolean enrichTaxon(HashMap<Class, Integer> errorCounts, Node taxonNode, TaxonPropertyLookupService
            service, Integer errorCount, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
        try {
            if (lookupAndSetProperties(taxonNode, service, taxonName, properties)) {
                return true;
            }
            resetErrorCount(errorCounts, service);
        } catch (TaxonPropertyLookupServiceException ex) {
            LOG.warn("failed to find a match for [" + taxonName + "] in [" + service.getClass().getSimpleName() + "]", ex);
            incrementErrorCount(errorCounts, service, errorCount);
            throw new TaxonPropertyLookupServiceException("re-throwing", ex);
        }
        return false;
    }

    private void resetErrorCount(HashMap<Class, Integer> errorCounts, TaxonPropertyLookupService service) {
        errorCounts.put(service.getClass(), 0);
    }

    private boolean lookupAndSetProperties(Node taxonNode, TaxonPropertyLookupService service, String
            taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        service.lookupPropertiesByName(taxonName, properties);
        return properties != null && setProperties(taxonNode, properties);
    }

    private boolean setProperties(Node taxonNode, Map<String, String> properties) {
        boolean enrichedAtLeastOneProperty = false;
        Transaction transaction = graphDbService.beginTx();
        try {
            for (Map.Entry<String, String> property : properties.entrySet()) {
                if (property.getValue() != null) {
                    taxonNode.setProperty(property.getKey(), property.getValue());
                    enrichedAtLeastOneProperty = true;
                }
            }
            transaction.success();

        } finally {
            transaction.finish();
        }
        return enrichedAtLeastOneProperty;
    }

    private void incrementErrorCount(HashMap<Class, Integer> errorCounts, TaxonPropertyLookupService
            service, Integer errorCount) {
        if (errorCounts.containsKey(service.getClass()) && errorCount != null) {
            errorCounts.put(service.getClass(), ++errorCount);
        } else {
            resetErrorCount(errorCounts, service);
        }
    }

    private void shutdownServices() {
        for (TaxonPropertyLookupService service : services) {
            service.shutdown();
        }
        services.clear();
    }

    public void setServices(List<TaxonPropertyLookupService> services) {
        shutdownServices();
        this.services.addAll(services);
    }
}
