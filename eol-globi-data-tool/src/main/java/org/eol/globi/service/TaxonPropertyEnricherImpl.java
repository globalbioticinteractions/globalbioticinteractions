package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private final GraphDatabaseService graphDbService;

    public TaxonPropertyEnricherImpl(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
    }

    @Override
    public void enrich(Taxon taxon) throws IOException {
        doEnrichment(taxon);
    }

    private void doEnrichment(Taxon taxon) {
        Node taxonNode = taxon.getUnderlyingNode();
        Map<String, String> properties = new HashMap<String, String>();
        for (TaxonPropertyLookupService service : services) {
            try {
                enrichTaxonWithPropertyValue(errorCounts, taxonNode, service, properties);
            } catch (TaxonPropertyLookupServiceException e) {
                LOG.warn("problem with taxon lookup", e);
                service.shutdown();
            }
            properties.clear();
        }
    }

    private void enrichTaxonWithPropertyValue(HashMap<Class, Integer> errorCounts, Node
            taxonNode, TaxonPropertyLookupService service, Map<String, String> properties) throws
            TaxonPropertyLookupServiceException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
        } else {
            enrichTaxon(errorCounts, taxonNode, service, errorCount, properties);
        }
    }

    private void enrichTaxon(HashMap<Class, Integer> errorCounts, Node taxonNode, TaxonPropertyLookupService
            service, Integer errorCount, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
        try {
            lookupAndSetProperties(taxonNode, service, taxonName, properties);
            resetErrorCount(errorCounts, service);
        } catch (TaxonPropertyLookupServiceException ex) {
            LOG.warn("failed to find a match for [" + taxonName + "] in [" + service.getClass().getSimpleName() + "]", ex);
            incrementErrorCount(errorCounts, service, errorCount);
            throw new TaxonPropertyLookupServiceException("re-throwing", ex);
        }
    }

    private void resetErrorCount(HashMap<Class, Integer> errorCounts, TaxonPropertyLookupService service) {
        errorCounts.put(service.getClass(), 0);
    }

    private void lookupAndSetProperties(Node taxonNode, TaxonPropertyLookupService service, String
            taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        service.lookupPropertiesByName(taxonName, properties);
        if (properties.size() > 0) {
            setProperties(taxonNode, properties);
        }
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
