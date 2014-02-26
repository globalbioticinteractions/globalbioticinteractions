package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.domain.TaxonomyProvider.ID_PREFIX_EOL;
import static org.eol.globi.domain.TaxonomyProvider.ID_PREFIX_WIKIPEDIA;
import static org.eol.globi.domain.TaxonomyProvider.WIKIPEDIA;

public class FunctionalGroupService extends BaseTaxonIdService {

    public static final String PHYTOPLANKTON = ID_PREFIX_WIKIPEDIA + "Phytoplankton";
    public static final String PLANKTON = ID_PREFIX_WIKIPEDIA + "Plankton";
    public static final String ZOOPLANKTON = ID_PREFIX_WIKIPEDIA + "Zooplankton";
    public static final String INVERTEBRATA = ID_PREFIX_WIKIPEDIA + "Invertebrata";
    public static final String BACTERIOPLANKTON = ID_PREFIX_WIKIPEDIA + "Bacterioplankton";
    public static final String ALGAE = ID_PREFIX_EOL + "Algae";
    public static final String MACROALGAE = WIKIPEDIA + "Macroalgae";

    private Map<String, String> mapping = new HashMap<String, String>() {{
        put("Phytoplankton", PHYTOPLANKTON);
        put("Zooplankton", ZOOPLANKTON);
        put("Bacterioplankton", BACTERIOPLANKTON);
        put("Plankton", PLANKTON);
        put("Invertebrata", INVERTEBRATA);
        put("Algae", ALGAE);
        put("Macroalgae", MACROALGAE);
    }};

    private Map<String, String> pathLookup = new HashMap<String, String>() {{
        put(PHYTOPLANKTON, "Plankton | Phytoplankton");
        put(ZOOPLANKTON, "Plankton | Zooplankton | Animalia");
        put(BACTERIOPLANKTON, "Plankton | Bacterioplankton | Bacteria");
        put(PLANKTON, "Plankton");
        put(INVERTEBRATA, "Invertebrata");
        put(ALGAE, "Algae");
        put(MACROALGAE, "Algae | Macroalgae");
    }};

    public String lookupIdByName(String taxonName) throws TaxonPropertyLookupServiceException {
        String id = null;
        String lowerCaseName = StringUtils.lowerCase(taxonName);
        if (StringUtils.isNotBlank(lowerCaseName)) {
            id = mapping.get(lowerCaseName);
        }
        return id;
    }

    @Override
    public String lookupTaxonPathById(String id) throws TaxonPropertyLookupServiceException {
        return pathLookup.get(id);
    }
}
