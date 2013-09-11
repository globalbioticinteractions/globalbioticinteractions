package org.eol.globi.service;

import org.eol.globi.data.taxon.GulfBaseTaxonParser;
import org.eol.globi.data.taxon.GulfBaseTaxonReaderFactory;
import org.eol.globi.data.taxon.TaxonTerm;
import org.eol.globi.data.taxon.TaxonomyImporter;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;

import java.util.List;
import java.util.Map;

public class GulfBaseService extends OfflineService {

    @Override
    protected TaxonomyImporter createTaxonomyImporter() {
        return new TaxonomyImporter(new GulfBaseTaxonParser(), new GulfBaseTaxonReaderFactory());
    }

    @Override
    protected String getValueForPropertyName(String propertyName, TaxonTerm term) {
        String value = null;
        if (NodeBacked.EXTERNAL_ID.equals(propertyName)) {
            value = TaxonomyProvider.ID_PREFIX_GULFBASE + term.getId();
        } else if (Taxon.PATH.equals(propertyName)) {
            value = term.getRankPath();
        }
        return value;
    }

}
