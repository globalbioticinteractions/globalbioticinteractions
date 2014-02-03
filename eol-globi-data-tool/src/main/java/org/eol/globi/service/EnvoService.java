package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.domain.TaxonomyProvider.ID_PREFIX_ENVO;

public class EnvoService extends BaseTaxonIdService {

    public static final String SEDIMENT = ID_PREFIX_ENVO + "00002007";
    public static final String SOIL = ID_PREFIX_ENVO + "00001998";
    public static final String ORGANIC_MATERIAL = ID_PREFIX_ENVO + "01000155";
    public static final String FECES = ID_PREFIX_ENVO + "00002003";
    public static final String WOOD = ID_PREFIX_ENVO + "00002040";
    public static final String ROCK = ID_PREFIX_ENVO + "00001995";

    private Map<String, String> mapping = new HashMap<String, String>() {{
        put("organic material", ORGANIC_MATERIAL);
        put("detritus", ORGANIC_MATERIAL);
        put("organic detritus", ORGANIC_MATERIAL);
        put("organic matter", ORGANIC_MATERIAL);
        put("Unidentified remains", ORGANIC_MATERIAL);
        put("suspended organic matter   ", ORGANIC_MATERIAL);
        put("dissolved organic carbon", ORGANIC_MATERIAL);
        put("organic matter in mud", ORGANIC_MATERIAL);
        put("dung", FECES);
        put("animal dung", FECES);
        put("bovine or equine dung", FECES);
        put("rock", ROCK);
        put("organic matter", ORGANIC_MATERIAL);
        put("wood", WOOD);
        put("rotting wood", WOOD);
        put("sediment POC", SEDIMENT);
        put("sediment", SEDIMENT);
        put("soil", SOIL);
    }};

    private Map<String, String> pathLookup = new HashMap<String, String>() {{
        put(SEDIMENT, "environmental material | sediment");
        put(SOIL, "environmental material | soil");
        put(ORGANIC_MATERIAL, "environmental material | organic material");
        put(FECES, "environmental material | organic material | bodily fluid | excreta | feces");
        put(WOOD, "environmental material | organic material | wood");
        put(ROCK, "environmental material");
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
