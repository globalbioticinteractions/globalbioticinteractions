package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;

import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.domain.TaxonomyProvider.ID_PREFIX_ENVO;

public class EnvoService implements PropertyEnricher {

    public static final String SEDIMENT = ID_PREFIX_ENVO + "00002007";
    public static final String SOIL = ID_PREFIX_ENVO + "00001998";
    public static final String ORGANIC_MATERIAL = ID_PREFIX_ENVO + "01000155";
    public static final String FECES = ID_PREFIX_ENVO + "00002003";
    public static final String WOOD = ID_PREFIX_ENVO + "00002040";
    public static final String PLASTIC = ID_PREFIX_ENVO + "01000404";
    public static final String ROCK = ID_PREFIX_ENVO + "00001995";
    public static final String PIECE_OF_ROCK = ID_PREFIX_ENVO + "00000339";
    public static final String DETRITUS = ID_PREFIX_ENVO + "01001103";

    private Map<String, String> mapping = new HashMap<String, String>() {{
        put("organic material", ORGANIC_MATERIAL);
        put("detritus", DETRITUS);
        put("organic detritus", DETRITUS);
        put("organic matter", ORGANIC_MATERIAL);
        put("Unidentified remains", ORGANIC_MATERIAL);
        put("suspended organic matter   ", ORGANIC_MATERIAL);
        put("dissolved organic carbon", ORGANIC_MATERIAL);
        put("organic matter in mud", ORGANIC_MATERIAL);
        put("dung", FECES);
        put("animal dung", FECES);
        put("bovine or equine dung", FECES);
        put("plastic", PLASTIC);
        put("rock", ROCK);
        put("organic matter", ORGANIC_MATERIAL);
        put("wood", WOOD);
        put("rotting wood", WOOD);
        put("sediment POC", SEDIMENT);
        put("sediment", SEDIMENT);
        put("soil", SOIL);
        put("stones", PIECE_OF_ROCK);
    }};

    private Map<String, String[]> pathLookup = new HashMap<String, String[]>() {{
        put(SEDIMENT, new String[]{"environmental material | sediment", "ENVO:00010483 | ENVO:00002007"});
        put(SOIL, new String[]{"environmental material | soil", "ENVO:00010483 | ENVO:00001998"});
        put(ORGANIC_MATERIAL, new String[]{"environmental material | organic material", "ENVO:00010483 | ENVO:01000155"});
        put(DETRITUS, new String[]{"environmental material | organic material | detritus", "ENVO:00010483 | ENVO:01000155 | ENVO:01001103"});
        put(FECES, new String[]{"environmental material | organic material | bodily fluid | excreta | feces", "ENVO:00010483 | ENVO:01000155 | ENVO:02000019 | ENVO:02000022 | ENVO:00002003"});
        put(WOOD, new String[]{"environmental material | organic material | wood", "ENVO:00010483 | ENVO:01000155 | ENVO:00002040"});
        put(PLASTIC, new String[]{"environmental material | anthropogenic environmental material | plastic", "ENVO:00010483 | ENVO:0010001 | ENVO:01000404"});
        put(ROCK, new String[]{"environmental material | solid environmental material | rock", "ENVO:00010483 | ENVO:01000814 | ENVO:00001995"});
        put(PIECE_OF_ROCK, new String[]{"environmental feature | mesoscopic physical object | abiotic mesoscopic physical object | piece of rock", "ENVO:00002297 | ENVO:00002004 | ENVO:01000010 | ENVO:00000339"});
    }};

    private String lookupIdByName(String taxonName) throws PropertyEnricherException {
        String id = null;
        String lowerCaseName = StringUtils.lowerCase(taxonName);
        if (StringUtils.isNotBlank(lowerCaseName)) {
            id = mapping.get(lowerCaseName);
        }
        return id;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Taxon taxon = TaxonUtil.mapToTaxon(properties);
        if (StringUtils.startsWith(taxon.getExternalId(), TaxonomyProvider.ID_PREFIX_ENVO)) {
            populatePath(taxon);
        } else if (StringUtils.isNotBlank(taxon.getName())) {
            String externalId = lookupIdByName(taxon.getName());
            taxon.setExternalId(externalId);
            populatePath(taxon);
        }
        return TaxonUtil.taxonToMap(taxon);
    }

    @Override
    public void shutdown() {

    }

    protected void populatePath(Taxon taxon) {
        String[] path = pathLookup.get(taxon.getExternalId());
        if (path != null && path.length > 1) {
            taxon.setPath(path[0]);
            taxon.setPathIds(path[1]);
            if (StringUtils.isBlank(taxon.getName())) {
                String[] split = StringUtils.splitPreserveAllTokens(taxon.getPath(), CharsetConstant.SEPARATOR_CHAR);
                if (split != null) {
                    taxon.setName(StringUtils.trim(split[split.length - 1]));
                }
            }
        }
    }
}
