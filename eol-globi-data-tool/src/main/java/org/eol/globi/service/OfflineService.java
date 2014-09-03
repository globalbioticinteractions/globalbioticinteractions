package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.taxon.TaxonLookupService;
import org.eol.globi.data.taxon.TaxonomyImporter;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class OfflineService implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(OfflineService.class);
    private TaxonLookupService taxonLookupService;

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        for (String propertyName : properties.keySet()) {
            if (properties.get(propertyName) == null) {
                lookupProperty(properties.get(PropertyAndValueDictionary.NAME), properties, propertyName);
            }
        }
        return Collections.unmodifiableMap(new HashMap<String, String>(properties));
    }

    private void lookupProperty(String taxonName, Map<String, String> properties, String propertyName) throws PropertyEnricherException {
        if (null == taxonLookupService) {
            lazyInit();
        }
        try {
            Taxon[] taxonTerms = taxonLookupService.lookupTermsByName(taxonName);
            Taxon first = taxonTerms.length == 0 ? null : taxonTerms[0];
            if (taxonTerms.length > 1) {
                LOG.warn("found more than one matches for name [" + taxonName + "] in [" + getServiceName() + "], choosing first one with id [" + first.getExternalId() + "]");
            }
            String propertyValue = null;
            if (first != null) {
                propertyValue = getValueForPropertyName(propertyName, first);
            }
            if (propertyValue != null) {
                properties.put(propertyName, propertyValue);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("lookup for property with name [" + propertyName + "] failed for [" + getServiceName() + "].", e);
        }
    }

    protected String getServiceName() {
        return getClass().getSimpleName();
    }

    protected abstract String getValueForPropertyName(String propertyName, Taxon first);

    private void lazyInit() throws PropertyEnricherException {
        LOG.info("lazy init of taxonomy index [" + getServiceName() + "] started...");
        TaxonomyImporter importer = createTaxonomyImporter();
        try {
            importer.doImport();
        } catch (StudyImporterException e) {
            throw new PropertyEnricherException("failed to build index for [" + getServiceName() + "]", e);
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

    public String lookupPropertyValueByTaxonName(String taxonName, final String propertyName) throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put(propertyName, null);
        }};
        properties.put(PropertyAndValueDictionary.NAME, taxonName);
        enrich(properties);
        return properties.get(propertyName);
    }
}
