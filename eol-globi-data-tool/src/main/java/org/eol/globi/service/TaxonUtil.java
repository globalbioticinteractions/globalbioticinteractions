package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

import java.util.HashMap;
import java.util.Map;

public class TaxonUtil {
    public static Map<String, String> taxonToMap(Taxon taxon) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, taxon.getName());
        properties.put(PropertyAndValueDictionary.RANK, taxon.getRank());
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, taxon.getExternalId());
        properties.put(PropertyAndValueDictionary.PATH, taxon.getPath());
        properties.put(PropertyAndValueDictionary.PATH_NAMES, taxon.getPathNames());
        properties.put(PropertyAndValueDictionary.COMMON_NAMES, taxon.getCommonNames());
        return properties;
    }

    public static void mapToTaxon(Map<String, String> properties, Taxon taxon) {
        taxon.setName(properties.get(PropertyAndValueDictionary.NAME));
        taxon.setRank(properties.get(PropertyAndValueDictionary.RANK));
        taxon.setExternalId(properties.get(PropertyAndValueDictionary.EXTERNAL_ID));
        taxon.setPath(properties.get(PropertyAndValueDictionary.PATH));
        taxon.setPathNames(properties.get(PropertyAndValueDictionary.PATH_NAMES));
        taxon.setCommonNames(properties.get(PropertyAndValueDictionary.COMMON_NAMES));
    }
}
