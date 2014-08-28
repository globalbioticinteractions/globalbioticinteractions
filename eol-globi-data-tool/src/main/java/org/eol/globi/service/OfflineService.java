package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.taxon.TaxonLookupService;
import org.eol.globi.data.taxon.TaxonTerm;
import org.eol.globi.data.taxon.TaxonomyImporter;
import org.eol.globi.domain.PropertyAndValueDictionary;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class OfflineService implements TaxonPropertyLookupService {
    private static final Log LOG = LogFactory.getLog(OfflineService.class);
    private TaxonLookupService taxonLookupService;

    @Override
    public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        for (String propertyName : properties.keySet()) {
            if (properties.get(propertyName) == null) {
                lookupProperty(name, properties, propertyName);
            }
        }
    }

    @Override
    public void lookupProperties(Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        lookupPropertiesByName(properties.get(PropertyAndValueDictionary.NAME), properties);
    }

    private void lookupProperty(String taxonName, Map<String, String> properties, String propertyName) throws TaxonPropertyLookupServiceException {
        if (null == taxonLookupService) {
            lazyInit();
        }
        try {
            TaxonTerm[] taxonTerms = taxonLookupService.lookupTermsByName(taxonName);
            TaxonTerm first = taxonTerms.length == 0 ? null : taxonTerms[0];
            if (taxonTerms.length > 1) {
                LOG.warn("found more than one matches for name [" + taxonName + "] in [" + getServiceName() + "], choosing first one with id [" + first.getId() + "]");
            }
            String propertyValue = null;
            if (first != null) {
                propertyValue = getValueForPropertyName(propertyName, first);
            }
            if (propertyValue != null) {
                properties.put(propertyName, propertyValue);
            }
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("lookup for property with name [" + propertyName + "] failed for [" + getServiceName() + "].", e);
        }
    }

    protected String getServiceName() {
        return getClass().getSimpleName();
    }

    protected abstract String getValueForPropertyName(String propertyName, TaxonTerm first);

    private void lazyInit() throws TaxonPropertyLookupServiceException {
        LOG.info("lazy init of taxonomy index [" + getServiceName() + "] started...");
        TaxonomyImporter importer = createTaxonomyImporter();
        try {
            importer.doImport();
        } catch (StudyImporterException e) {
            throw new TaxonPropertyLookupServiceException("failed to build index for [" + getServiceName() + "]", e);
        }
        taxonLookupService = importer.getTaxonLookupService();
        LOG.info("lazy init of taxonomy index [" + getServiceName() + "] done.");
    }

    protected abstract TaxonomyImporter createTaxonomyImporter();

    @Override
    public void shutdown() {
        if (taxonLookupService != null) {
            taxonLookupService.destroy();
        }
    }

    public String lookupPropertyValueByTaxonName(String taxonName, final String propertyName) throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put(propertyName, null);
        }};
        lookupPropertiesByName(taxonName, properties);
        return properties.get(propertyName);
    }
}
