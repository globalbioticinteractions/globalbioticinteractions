package org.eol.globi.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoRegionFinderFactoryImpl implements EcoRegionFinderFactory {

    @Override
    public EcoRegionFinder createEcoRegionFinder(EcoRegionType type) {
        Map<EcoRegionType, EcoRegionFinderConfig> urlTypeMap = getUrlTypeMap();
        return new EcoRegionFinderImpl(urlTypeMap.get(type));
    }

    @Override
    public List<EcoRegionFinder> createAll() {
        List<EcoRegionFinder> finders = new ArrayList<EcoRegionFinder>();
        for (EcoRegionType ecoRegionType : EcoRegionType.values()) {
            finders.add(createEcoRegionFinder(ecoRegionType));
        }
        return finders;
    }

    private Map<EcoRegionType, EcoRegionFinderConfig> getUrlTypeMap() {
        return new HashMap<EcoRegionType, EcoRegionFinderConfig>() {{
            // Terrestrial Ecosystem of the World
            // http://maps.tnc.org/files/metadata/TerrEcos.xml
            // http://maps.tnc.org/files/shp/terr-ecoregions-TNC.zip
            EcoRegionFinderConfig config = new EcoRegionFinderConfig();
            config.setShapeFilePath("/teow-tnc/tnc_terr_ecoregions.shp");
            config.setNameLabel("ECO_NAME");
            config.setIdLabel("ECO_ID_U");
            config.setNamespace("TEOW");
            config.setPathLabels(new String[]{config.getNameLabel(), "WWF_MHTNAM", "WWF_REALM2"});
            put(EcoRegionType.Terrestrial, config);
            config.setGeometryLabel("the_geom");

            // http://maps.tnc.org/gis_data.html
            // Marine Ecosystems of the World (MEOW) http://maps.tnc.org/files/metadata/MEOW.xml
            // http://maps.tnc.org/files/shp/MEOW-TNC.zip
            config = new EcoRegionFinderConfig();
            config.setShapeFilePath("/meow-tnc/meow_ecos.shp");
            config.setNameLabel("ECOREGION");
            config.setIdLabel("ECO_CODE");
            config.setNamespace("MEOW");
            config.setPathLabels(new String[]{config.getNameLabel(), "PROVINCE", "REALM", "Lat_Zone"});
            config.setGeometryLabel("the_geom");
            put(EcoRegionType.Marine, config);

            // Fresh Water Ecosystems of the World (FEW) http://www.feow.org/
            // http://maps.tnc.org/files/metadata/FEOW.xml
            // http://maps.tnc.org/files/shp/FEOW-TNC.zip
            config = new EcoRegionFinderConfig();
            config.setShapeFilePath("/feow-tnc/FEOWv1_TNC.shp");
            config.setNameLabel("ECOREGION");
            config.setIdLabel("ECO_ID_U");
            config.setNamespace("FEOW");
            config.setPathLabels(new String[]{config.getNameLabel(), "MHT_TXT"});
            config.setGeometryLabel("the_geom");
            put(EcoRegionType.Freshwater, config);
        }};
    }
}
