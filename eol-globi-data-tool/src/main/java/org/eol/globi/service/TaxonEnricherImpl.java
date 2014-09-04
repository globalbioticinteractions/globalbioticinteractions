package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxonEnricherImpl implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonEnricherImpl.class);

    private final List<PropertyEnricher> services = new ArrayList<PropertyEnricher>();
    private final HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        for (PropertyEnricher service : services) {
            try {
                enrichedProperties = enrichTaxonWithPropertyValue(errorCounts, service, enrichedProperties);
                if (TaxonUtil.isResolved(enrichedProperties)) {
                    break;
                }
            } catch (PropertyEnricherException e) {
                LOG.warn("problem with taxon lookup", e);
                service.shutdown();
            }
        }
        return Collections.unmodifiableMap(enrichedProperties);
    }

    @Override
    public void shutdown() {
        for (PropertyEnricher service : services) {
            service.shutdown();
        }
        services.clear();
    }

    private Map<String, String> enrichTaxonWithPropertyValue(HashMap<Class, Integer> errorCounts, PropertyEnricher service, Map<String, String> properties) throws
            PropertyEnricherException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
        } else {
            properties = enrichTaxon(errorCounts, service, errorCount, properties);
        }
        return Collections.unmodifiableMap(properties);
    }

    private Map<String, String> enrichTaxon(HashMap<Class, Integer> errorCounts, PropertyEnricher
            service, Integer errorCount, Map<String, String> properties) throws PropertyEnricherException {
        try {
            Map<String, String> enrichedProperties = service.enrich(properties);
            resetErrorCount(errorCounts, service);
            return enrichedProperties;
        } catch (PropertyEnricherException ex) {
            LOG.warn("failed to find a match for [" + properties + "] in [" + service.getClass().getSimpleName() + "]", ex);
            incrementErrorCount(errorCounts, service, errorCount);
            throw new PropertyEnricherException("re-throwing", ex);
        }
    }

    private void resetErrorCount(HashMap<Class, Integer> errorCounts, PropertyEnricher service) {
        errorCounts.put(service.getClass(), 0);
    }

    private void incrementErrorCount(HashMap<Class, Integer> errorCounts, PropertyEnricher
            service, Integer errorCount) {
        if (errorCounts.containsKey(service.getClass()) && errorCount != null) {
            errorCounts.put(service.getClass(), ++errorCount);
        } else {
            resetErrorCount(errorCounts, service);
        }
    }

    private void shutdownServices() {
        for (PropertyEnricher service : services) {
            service.shutdown();
        }
        services.clear();
    }

    public void setServices(List<PropertyEnricher> services) {
        shutdownServices();
        this.services.addAll(services);
    }


}
