package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.taxon.TaxonLookupService;
import org.eol.globi.data.taxon.TaxonTerm;
import org.eol.globi.data.taxon.TaxonomyImporter;
import org.eol.globi.domain.Taxon;

import java.io.IOException;

public abstract class OfflineService implements TaxonPropertyLookupService {
    private static final Log LOG = LogFactory.getLog(GulfBaseService.class);
    private TaxonLookupService taxonLookupService;

    @Override
    public String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException {
        if (canLookupProperty(propertyName)) {
            if (null == taxonLookupService) {
                lazyInit();
            }
        } else {
            throw new TaxonPropertyLookupServiceException("lookup for property with name [" + propertyName + "] not supported.");
        }

        try {
            TaxonTerm[] taxonTerms = taxonLookupService.lookupTermsByName(taxonName);
            TaxonTerm first = taxonTerms.length == 0 ? null : taxonTerms[0];
            if (taxonTerms.length > 1) {
                LOG.warn("found more than one matches for name [" + taxonName + "], choosing first one with id [" + first.getId() + "]");
            }
            String value = null;
            if (first != null) {
                value = getValueForPropertyName(propertyName, first);
            }
            return value;
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("lookup for property with name [" + propertyName + "] failed.", e);
        }
    }

    protected abstract String getValueForPropertyName(String propertyName, TaxonTerm first);

    private void lazyInit() throws TaxonPropertyLookupServiceException {
        LOG.info("lazy init of taxonomy index started...");
        TaxonomyImporter importer = createTaxonomyImporter();
        try {
            importer.doImport();
        } catch (StudyImporterException e) {
            throw new TaxonPropertyLookupServiceException("failed to build index", e);
        }
        taxonLookupService = importer.getTaxonLookupService();
        LOG.info("lazy init of taxonomy index done.");
    }

    protected abstract TaxonomyImporter createTaxonomyImporter();

    @Override
    public abstract boolean canLookupProperty(String propertyName);

    @Override
    public void shutdown() {
        if (taxonLookupService != null) {
            taxonLookupService.destroy();
        }
    }
}
