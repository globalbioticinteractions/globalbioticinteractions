package org.eol.globi.service;

import org.eol.globi.geo.EcoRegionFinderImpl;
import org.eol.globi.geo.GeoUtil;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class TaxonRichnessLookup {
    private FileDataStore dataStore = null;

    public Double lookupRichness(double latitude, double longitude) throws IOException {
        lazyInit();
        SimpleFeatureSource features = dataStore.getFeatureSource();
        Map<String, Object> featureProperties = EcoRegionFinderImpl.getFeatureProperties(GeoUtil.getPoint(latitude, longitude), features.getFeatures());
        Double allNorm = null;
        if (featureProperties != null) {
            allNorm = (Double)featureProperties.get("AllNorm");
        }
        return allNorm;
    }

    private void lazyInit() throws IOException {
        if (dataStore == null) {
            String shapeFilePath = "/org/eol/globi/biodiversity/Global_patterns_predictors_marine_biodiversity_across_taxa.shp";
            URL dataStoreURL = getClass().getResource(shapeFilePath);
            dataStore = FileDataStoreFinder.getDataStore(dataStoreURL);
        }
    }

    public void dispose() {
        if (dataStore != null) {
            dataStore.dispose();
            dataStore = null;
        }
    }
}
