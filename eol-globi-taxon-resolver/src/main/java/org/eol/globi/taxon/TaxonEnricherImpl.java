package org.eol.globi.taxon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.Version;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.DateUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TaxonEnricherImpl implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonEnricherImpl.class);

    private final List<PropertyEnricher> services = new ArrayList<PropertyEnricher>();
    private final HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();
    private boolean hasLoggedError = false;

    private Date date = null;

    Date getDate() {
        return date == null ? new Date() : date;
    }

    void setDate(Date date) {
        this.date = date;
    }

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        for (PropertyEnricher service : services) {
            try {
                enrichedProperties = enrichTaxonWithPropertyValue(errorCounts, service, properties);
                if (TaxonUtil.isResolved(enrichedProperties)) {
                    enrichedProperties = new TreeMap<String, String>(enrichedProperties) {
                        {
                            put(PropertyAndValueDictionary.NAME_SOURCE, service.getClass().getSimpleName());
                            Version.getGitHubBaseUrl();
                            put(PropertyAndValueDictionary.NAME_SOURCE_URL, Version.getGitHubBaseUrl() + service.getClass().getName().replace(".", "/") + ".java");
                            put(PropertyAndValueDictionary.NAME_SOURCE_ACCESSED_AT, DateUtil.printDate(getDate()));
                        }
                    };
                    break;
                }
            } catch (PropertyEnricherException e) {
                LOG.warn("problem with taxon lookup", e);
                service.shutdown();
            }
        }
        return TaxonUtil.isResolved(enrichedProperties) ? Collections.unmodifiableMap(enrichedProperties) : properties;
    }

    @Override
    public void shutdown() {
        services.forEach(PropertyEnricher::shutdown);
        services.clear();
    }

    private Map<String, String> enrichTaxonWithPropertyValue(Map<Class, Integer> errorCounts, PropertyEnricher service, Map<String, String> properties) throws
            PropertyEnricherException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            if (!hasLoggedError) {
                LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
                hasLoggedError = true;
            }
        } else {
            properties = enrichTaxon(errorCounts, service, errorCount, properties);
        }
        return Collections.unmodifiableMap(properties);
    }

    private Map<String, String> enrichTaxon(Map<Class, Integer> errorCounts, PropertyEnricher
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

    private void resetErrorCount(Map<Class, Integer> errorCounts, PropertyEnricher service) {
        errorCounts.put(service.getClass(), 0);
    }

    private void incrementErrorCount(Map<Class, Integer> errorCounts, PropertyEnricher
            service, Integer errorCount) {
        if (errorCounts.containsKey(service.getClass()) && errorCount != null) {
            errorCounts.put(service.getClass(), ++errorCount);
        } else {
            resetErrorCount(errorCounts, service);
        }
    }

    public void setServices(List<PropertyEnricher> services) {
        shutdown();
        this.services.addAll(services);
    }


}
