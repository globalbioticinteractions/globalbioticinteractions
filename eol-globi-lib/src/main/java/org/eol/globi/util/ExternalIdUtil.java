package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.TaxonomyProvider;

import java.util.HashMap;
import java.util.Map;

public class ExternalIdUtil {
    public static String infoURLForExternalId(String externalId) {
        String url = null;
        if (externalId != null) {
            for (Map.Entry<String, String> idPrefixToUrlPrefix : getURLPrefixMap().entrySet()) {
                if (externalId.startsWith(idPrefixToUrlPrefix.getKey())) {
                    url = idPrefixToUrlPrefix.getValue() + externalId.replaceAll(idPrefixToUrlPrefix.getKey(), "");
                }
                if (url != null) {
                    break;
                }
            }
        }
        return url;
    }

    public static Map<String, String> getURLPrefixMap() {
        return new HashMap<String, String>() {{
            put(TaxonomyProvider.ID_PREFIX_EOL, "http://eol.org/pages/");
            put(TaxonomyProvider.ID_PREFIX_WORMS, "http://www.marinespecies.org/aphia.php?p=taxdetails&id=");
            put(TaxonomyProvider.ID_PREFIX_ENVO, "http://purl.obolibrary.org/obo/ENVO_");
            put(TaxonomyProvider.ID_PREFIX_WIKIPEDIA, "http://wikipedia.org/wiki/");
            put(TaxonomyProvider.ID_PREFIX_GULFBASE, "http://gulfbase.org/biogomx/biospecies.php?species=");
            put(TaxonomyProvider.ID_PREFIX_GAME, "http://research.myfwc.com/game/Survey.aspx?id=");
            put(TaxonomyProvider.ID_CMECS, "http://cmecscatalog.org/classification/aquaticSetting/");
            put(TaxonomyProvider.ID_PREFIX_GBIF, "http://www.gbif.org/species/");
            put(TaxonomyProvider.ID_PREFIX_INATURALIST, "http://inaturalist.org/observations/");
            put(TaxonomyProvider.ID_PREFIX_HTTP, TaxonomyProvider.ID_PREFIX_HTTP);
        }};
    }

    public static boolean isSupported(String externalId) {
        boolean supported = false;
        for (String prefix : getURLPrefixMap().values()) {
            if (StringUtils.startsWith(externalId, prefix)) {
                supported = true;
            }
        }
        return supported;
    }
}
