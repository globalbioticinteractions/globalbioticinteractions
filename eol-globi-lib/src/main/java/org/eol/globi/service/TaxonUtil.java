package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TaxonUtil {
    public static Map<String, String> taxonToMap(Taxon taxon) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, taxon.getName());
        properties.put(PropertyAndValueDictionary.RANK, taxon.getRank());
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, taxon.getExternalId());
        properties.put(PropertyAndValueDictionary.PATH, taxon.getPath());
        properties.put(PropertyAndValueDictionary.PATH_IDS, taxon.getPathIds());
        properties.put(PropertyAndValueDictionary.PATH_NAMES, taxon.getPathNames());
        properties.put(PropertyAndValueDictionary.COMMON_NAMES, taxon.getCommonNames());
        return Collections.unmodifiableMap(properties);
    }

    public static void mapToTaxon(Map<String, String> properties, Taxon taxon) {
        taxon.setName(properties.get(PropertyAndValueDictionary.NAME));
        taxon.setRank(properties.get(PropertyAndValueDictionary.RANK));
        taxon.setExternalId(properties.get(PropertyAndValueDictionary.EXTERNAL_ID));
        taxon.setPath(properties.get(PropertyAndValueDictionary.PATH));
        taxon.setPathIds(properties.get(PropertyAndValueDictionary.PATH_IDS));
        taxon.setPathNames(properties.get(PropertyAndValueDictionary.PATH_NAMES));
        taxon.setCommonNames(properties.get(PropertyAndValueDictionary.COMMON_NAMES));
    }

    public static boolean isResolved(Map<String, String> properties) {
        return StringUtils.isNotBlank(properties.get(PropertyAndValueDictionary.NAME))
                && StringUtils.isNotBlank(properties.get(PropertyAndValueDictionary.EXTERNAL_ID))
                && StringUtils.isNotBlank(properties.get(PropertyAndValueDictionary.PATH));
    }

    public static Taxon enrich(PropertyEnricher enricher, Taxon taxon) throws PropertyEnricherException {
        Map<String, String> properties = taxonToMap(taxon);
        Taxon enrichedTaxon = new TaxonImpl();
        mapToTaxon(enricher.enrich(properties), enrichedTaxon);
        return enrichedTaxon;
    }

    public static Taxon mapToTaxon(Map<String, String> properties) {
        Taxon taxon = new TaxonImpl();
        mapToTaxon(properties, taxon);
        return taxon;
    }

    public static Taxon copy(Taxon taxon) {
        TaxonImpl taxonCopy = new TaxonImpl();
        mapToTaxon(taxonToMap(taxon), taxonCopy);
        return taxonCopy;
    }

    public static boolean likelyHomonym(Taxon taxonA, Taxon taxonB) {
        boolean likelyHomonym = false;
        Map<String, String> pathMapA = toPathMap(taxonA);
        Map<String, String> pathMapB = toPathMap(taxonB);
        String[] ranks = new String[]{"kingdom", "phylum", "class"};
        for (String rank : ranks) {
            if (pathMapA.containsKey(rank) && pathMapB.containsKey(rank)) {
                if (!StringUtils.equals(pathMapA.get(rank), pathMapB.get(rank))) {
                    likelyHomonym = true;
                }
            }
        }
        return likelyHomonym;
    }

    protected static Map<String, String> toPathMap(Taxon taxonA) {
        String[] pathNames = StringUtils.split(taxonA.getPathNames(), CharsetConstant.SEPARATOR_CHAR);
        String[] path = StringUtils.split(taxonA.getPath(), CharsetConstant.SEPARATOR_CHAR);
        Map<String, String> pathMap = new HashMap<String, String>();
        if (pathNames != null && path != null && pathNames.length == path.length) {
            for (int i = 0; i < pathNames.length; i++) {
                pathMap.put(StringUtils.trim(StringUtils.lowerCase(pathNames[i])), StringUtils.trim(path[i]));
            }
        }
        return pathMap;
    }
}
