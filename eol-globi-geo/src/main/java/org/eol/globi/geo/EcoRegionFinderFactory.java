package org.eol.globi.geo;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class EcoRegionFinderFactory {

    private static Map<EcoRegionType, EcoRegionFinderConfig> typeUrlMap = new HashMap<EcoRegionType, EcoRegionFinderConfig>() {{
        // Terrestrial Ecosystem of the World
        // http://maps.tnc.org/files/metadata/TerrEcos.xml
        // http://maps.tnc.org/files/shp/terr-ecoregions-TNC.zip
        EcoRegionFinderConfig config = new EcoRegionFinderConfig();
        config.shapeFileURL = getDataStoreURLForShapeFile("/teow-tnc/tnc_terr_ecoregions.shp");
        config.nameLabel = "ECO_NAME";
        config.idLabel = "ECO_ID_U";
        config.namespace = "TEOW";
        config.pathLabels = new String[]{config.nameLabel, "WWF_MHTNAM", "WWF_REALM2"};
        put(EcoRegionType.Terrestrial, config);

        // http://maps.tnc.org/gis_data.html
        // Marine Ecosystems of the World (MEOW) http://maps.tnc.org/files/metadata/MEOW.xml
        // http://maps.tnc.org/files/shp/MEOW-TNC.zip
        config = new EcoRegionFinderConfig();
        config.shapeFileURL = getDataStoreURLForShapeFile("/meow-tnc/meow_ecos.shp");
        config.nameLabel = "ECOREGION";
        config.idLabel = "ECO_CODE";
        config.namespace = "MEOW";
        config.pathLabels = new String[]{config.nameLabel, "PROVINCE", "REALM", "Lat_Zone"};
        put(EcoRegionType.Marine, config);

        // Fresh Water Ecosystems of the World (FEW) http://www.feow.org/
        // http://maps.tnc.org/files/metadata/FEOW.xml
        // http://maps.tnc.org/files/shp/FEOW-TNC.zip
        config = new EcoRegionFinderConfig();
        config.shapeFileURL = getDataStoreURLForShapeFile("/feow-tnc/FEOWv1_TNC.shp");
        config.nameLabel = "ECOREGION";
        config.idLabel = "ECO_ID_U";
        config.namespace = "FEOW";
        config.pathLabels = new String[]{config.nameLabel, "MHT_TXT"};
        put(EcoRegionType.Freshwater, config);
    }};

    private static URL getDataStoreURLForShapeFile(String shapeFile) {
        try {
            return EcoRegionFinderFactory.class.getResource(shapeFile).toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException("failed to find [" + shapeFile + "] ... did you run mvn install on the commandline to install shapefiles?");
        }
    }


    public EcoRegionFinder createEcoRegionFinder(EcoRegionType type) {
        return new EcoRegionFinderImpl(typeUrlMap.get(type));
    }
}
