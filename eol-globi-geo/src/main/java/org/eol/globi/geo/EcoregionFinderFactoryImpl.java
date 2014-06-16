package org.eol.globi.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoregionFinderFactoryImpl implements EcoregionFinderFactory {

    @Override
    public EcoregionFinder createEcoregionFinder(EcoregionType type) {
        Map<EcoregionType, EcoregionFinderConfig> urlTypeMap = getUrlTypeMap();
        return new EcoregionFinderImpl(urlTypeMap.get(type));
    }

    @Override
    public List<EcoregionFinder> createAll() {
        List<EcoregionFinder> finders = new ArrayList<EcoregionFinder>();
        for (EcoregionType type : EcoregionType.values()) {
            finders.add(createEcoregionFinder(type));
        }
        return finders;
    }

    private Map<EcoregionType, EcoregionFinderConfig> getUrlTypeMap() {
        return new HashMap<EcoregionType, EcoregionFinderConfig>() {{
            // Terrestrial Ecosystem of the World
            // http://maps.tnc.org/files/metadata/TerrEcos.xml
            // http://maps.tnc.org/files/shp/terr-ecoregions-TNC.zip
            EcoregionFinderConfig config = new EcoregionFinderConfig();
            config.setShapeFilePath("/teow-tnc/tnc_terr_ecoregions.shp");
            config.setNameLabel("ECO_NAME");
            config.setIdLabel("ECO_ID_U");
            config.setNamespace("TEOW");
            config.setPathLabels(new String[]{config.getNameLabel(), "WWF_MHTNAM", "WWF_REALM2"});
            put(EcoregionType.TERRESTRIAL_ECOREGIONS_OF_THE_WORLD, config);
            config.setGeometryLabel("the_geom");

            // http://maps.tnc.org/gis_data.html
            // Marine Ecosystems of the World (MEOW) http://maps.tnc.org/files/metadata/MEOW.xml
            // http://maps.tnc.org/files/shp/MEOW-TNC.zip
            config = new EcoregionFinderConfig();
            config.setShapeFilePath("/meow-tnc/meow_ecos.shp");
            config.setNameLabel("ECOREGION");
            config.setIdLabel("ECO_CODE");
            config.setNamespace("MEOW");
            config.setPathLabels(new String[]{config.getNameLabel(), "PROVINCE", "REALM", "Lat_Zone"});
            config.setGeometryLabel("the_geom");
            put(EcoregionType.MARINE_ECOREGIONS_OF_THE_WORLD, config);

            // Fresh Water Ecosystems of the World (FEW) http://www.feow.org/
            // http://maps.tnc.org/files/metadata/FEOW.xml
            // http://maps.tnc.org/files/shp/FEOW-TNC.zip
            config = new EcoregionFinderConfig();
            config.setShapeFilePath("/feow-tnc/FEOWv1_TNC.shp");
            config.setNameLabel("ECOREGION");
            config.setIdLabel("ECO_ID_U");
            config.setNamespace("FEOW");
            config.setPathLabels(new String[]{config.getNameLabel(), "MHT_TXT"});
            config.setGeometryLabel("the_geom");
            put(EcoregionType.FRESHWATER_ECOREGIONS_OF_THE_WORLD, config);

            // VLIZ (2009). Longhurst Biogeographical Provinces. Available online at http://www.marineregions.org/. Consulted on 2014-02-12.
            //
            // Longhurst, A.R et al. (1995). An estimate of global primary production in the ocean from satellite radiometer data. J. Plankton Res. 17, 1245-1271
            // Longhurst, A.R. (1995). Seasonal cycles of pelagic production and consumption. Prog. Oceanogr. 36, 77-167
            // Longhurst, A.R. (1998). Ecological Geography of the Sea. Academic Press, San Diego. 397p. (IMIS)
            // Longhurst, A.R. (2006). Ecological Geography of the Sea. 2nd Edition. Academic Press, San Diego, 560p.
            config = new EcoregionFinderConfig();
            config.setShapeFilePath("/longhurst/Longhurst_world_v4_2010.shp");
            config.setNameLabel("ProvDescr");
            config.setIdLabel("ProvCode");
            config.setNamespace("LBP");
            config.setPathLabels(new String[]{config.getNameLabel()});
            config.setGeometryLabel("the_geom");
            put(EcoregionType.LONGHURST_BIOGEOGRAPHICAL_PROVINCES, config);
        }};
    }
}
