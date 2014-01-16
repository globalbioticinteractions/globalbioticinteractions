package org.eol.globi.geo;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class EcoRegionFinderFactory {

    private static Map<EcoRegionType, URL> typeUrlMap = new HashMap<EcoRegionType, URL>() {{
        // Terrestrial Ecosystem of the World
        // http://maps.tnc.org/files/metadata/TerrEcos.xml
        // http://maps.tnc.org/files/shp/terr-ecoregions-TNC.zip
        put(EcoRegionType.Terrestrial, getDataStoreURLForShapeFile("/teow-tnc/tnc_terr_ecoregions.shp"));

        // http://maps.tnc.org/gis_data.html
        // Marine Ecosystems of the World (MEOW) http://maps.tnc.org/files/metadata/MEOW.xml
        // http://maps.tnc.org/files/shp/MEOW-TNC.zip
        put(EcoRegionType.Marine, getDataStoreURLForShapeFile("/meow-tnc/meow_ecos.shp"));

        // Fresh Water Ecosystems of the World (FEW) http://www.feow.org/
        // http://maps.tnc.org/files/metadata/FEOW.xml
        // http://maps.tnc.org/files/shp/FEOW-TNC.zip
        put(EcoRegionType.Freshwater, getDataStoreURLForShapeFile("/feow-tnc/FEOWv1_TNC.shp"));
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
