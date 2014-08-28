package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxonPropertyEnricherImpl implements TaxonPropertyEnricher, PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonPropertyEnricherImpl.class);

    private final List<PropertyEnricher> services = new ArrayList<PropertyEnricher>();
    private final HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();

    @Override
    public void enrich(Taxon taxon) throws PropertyEnricherException {
        Map<String, String> properties = TaxonUtil.taxonToMap(taxon);
        enrich(properties);
        TaxonUtil.mapToTaxon(properties, taxon);
    }

    @Override
    public void enrich(Map<String, String> properties) throws PropertyEnricherException {
        for (PropertyEnricher service : services) {
            try {
                enrichTaxonWithPropertyValue(errorCounts, service, properties);
                if (StringUtils.isNotBlank(properties.get(PropertyAndValueDictionary.NAME))
                        && StringUtils.isNotBlank(properties.get(PropertyAndValueDictionary.EXTERNAL_ID))
                        && StringUtils.isNotBlank(properties.get(PropertyAndValueDictionary.PATH))) {
                    break;
                }
            } catch (PropertyEnricherException e) {
                LOG.warn("problem with taxon lookup", e);
                service.shutdown();
            }
        }
    }

    @Override
    public void shutdown() {
        for (PropertyEnricher service : services) {
            service.shutdown();
        }
        services.clear();
    }

    private void enrichTaxonWithPropertyValue(HashMap<Class, Integer> errorCounts, PropertyEnricher service, Map<String, String> properties) throws
            PropertyEnricherException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
        } else {
            enrichTaxon(errorCounts, service, errorCount, properties);
        }
    }

    private void enrichTaxon(HashMap<Class, Integer> errorCounts, PropertyEnricher
            service, Integer errorCount, Map<String, String> properties) throws PropertyEnricherException {
        try {
            service.enrich(properties);
            resetErrorCount(errorCounts, service);
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
