package org.eol.globi.service;

import org.eol.globi.data.taxon.GulfBaseTaxonParser;
import org.eol.globi.data.taxon.GulfBaseTaxonReaderFactory;
import org.eol.globi.data.taxon.TaxonomyImporter;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;

public class GulfBaseService extends OfflineService {

    @Override
    protected TaxonomyImporter createTaxonomyImporter() {
        return new TaxonomyImporter(new GulfBaseTaxonParser(), new GulfBaseTaxonReaderFactory());
    }

    @Override
    protected String getValueForPropertyName(String propertyName, Taxon term) {
        String value = null;
        if (PropertyAndValueDictionary.EXTERNAL_ID.equals(propertyName)) {
            value = TaxonomyProvider.ID_PREFIX_GULFBASE + term.getExternalId();
        } else if (PropertyAndValueDictionary.PATH.equals(propertyName)) {
            value = term.getPath();
        }
        return value;
    }

}
