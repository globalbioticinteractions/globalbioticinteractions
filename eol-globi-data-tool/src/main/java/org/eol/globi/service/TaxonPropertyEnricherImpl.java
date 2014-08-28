package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxonPropertyEnricherImpl implements TaxonPropertyEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonPropertyEnricherImpl.class);

    private final List<TaxonPropertyLookupService> services = new ArrayList<TaxonPropertyLookupService>();
    private final HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();

    @Override
    public void enrich(Taxon taxon) {
        doEnrichment(taxon);
    }

    private void doEnrichment(Taxon taxon) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, taxon.getName());
        properties.put(PropertyAndValueDictionary.RANK, taxon.getRank());
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, taxon.getExternalId());
        properties.put(PropertyAndValueDictionary.PATH, taxon.getPath());
        properties.put(PropertyAndValueDictionary.PATH_NAMES, taxon.getPathNames());
        properties.put(PropertyAndValueDictionary.COMMON_NAMES, taxon.getCommonNames());
        for (TaxonPropertyLookupService service : services) {
            try {
                enrichTaxonWithPropertyValue(errorCounts, taxon, service, properties);
                if (TaxonMatchValidator.hasMatch(taxon)) {
                    break;
                }
            } catch (TaxonPropertyLookupServiceException e) {
                LOG.warn("problem with taxon lookup", e);
                service.shutdown();
            }
        }
    }

    private void enrichTaxonWithPropertyValue(HashMap<Class, Integer> errorCounts, Taxon
            taxon, TaxonPropertyLookupService service, Map<String, String> properties) throws
            TaxonPropertyLookupServiceException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
        } else {
            enrichTaxon(errorCounts, taxon, service, errorCount, properties);
        }
    }

    private void enrichTaxon(HashMap<Class, Integer> errorCounts, Taxon taxon, TaxonPropertyLookupService
            service, Integer errorCount, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        String taxonName = taxon.getName();
        try {
            lookupAndSetProperties(taxon, service, properties);
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

    private void lookupAndSetProperties(Taxon taxon, TaxonPropertyLookupService service,
             Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        service.lookupPropertiesByName(taxon.getName(), properties);
        if (properties.size() > 0) {
            setProperties(taxon, properties);
        }
    }

    private boolean setProperties(Taxon taxon, Map<String, String> properties) {
        boolean enrichedAtLeastOneProperty = false;
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (property.getValue() != null) {
                enrichedAtLeastOneProperty = false;
                if (PropertyAndValueDictionary.NAME.equals(property.getKey())) {
                    taxon.setName(property.getValue());
                } else if (PropertyAndValueDictionary.RANK.equals(property.getKey())) {
                    taxon.setRank(property.getValue());
                } else if (PropertyAndValueDictionary.COMMON_NAMES.equals(property.getKey())) {
                    taxon.setCommonNames(property.getValue());
                } else if (PropertyAndValueDictionary.PATH.equals(property.getKey())) {
                    taxon.setPath(property.getValue());
                } else if (PropertyAndValueDictionary.PATH_NAMES.equals(property.getKey())) {
                    taxon.setPathNames(property.getValue());
                } else if (PropertyAndValueDictionary.EXTERNAL_ID.equals(property.getKey())) {
                    taxon.setExternalId(property.getValue());
                } else {
                    enrichedAtLeastOneProperty = false;
                }

            }
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
