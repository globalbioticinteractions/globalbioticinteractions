package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class OfflineService implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(OfflineService.class);
    private TaxonLookupService taxonLookupService;

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        if (null == taxonLookupService) {
            lazyInit();
        }
        Map<String, String> enrichedProperties = enrichById(properties);
        if (enrichedProperties == null) {
            enrichedProperties = enrichByName(properties);
        }
        return enrichedProperties == null ? new HashMap<String, String>(properties) : enrichedProperties;
    }

    protected Map<String, String> enrichByName(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = null;
        String propertyName = PropertyAndValueDictionary.NAME;
        String taxonName = properties.get(propertyName);
        if (StringUtils.isNotBlank(taxonName)) {
            try {
                enrichedProperties = toEnrichedProperies(propertyName, taxonLookupService.lookupTermsByName(taxonName));
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to lookup [" + taxonName + "]", e);
            }
        }
        return enrichedProperties;
    }

    protected Map<String, String> enrichById(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = null;
        String propertyName = PropertyAndValueDictionary.EXTERNAL_ID;
        String taxonExternalId = properties.get(propertyName);
        if (StringUtils.isNotBlank(taxonExternalId)) {
            try {
                enrichedProperties = toEnrichedProperies(propertyName, taxonLookupService.lookupTermsById(taxonExternalId));
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to lookup [" + taxonExternalId + "]", e);
            }
        }
        return enrichedProperties;
    }

    protected Map<String, String> toEnrichedProperies(String propertyValue, Taxon[] taxa) {
        Map<String, String> enrichedProperties = null;
        Taxon first = taxa.length == 0 ? null : taxa[0];
        if (taxa.length > 1) {
            LOG.warn("found more than one matches for [" + propertyValue + "] in [" + getServiceName() + "], choosing first one with id [" + first.getExternalId() + "]");
        }
        if (first != null) {
            enrichedProperties = TaxonUtil.taxonToMap(first);
        }
        return enrichedProperties;
    }

    protected String getServiceName() {
        return getClass().getSimpleName();
    }

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
}
