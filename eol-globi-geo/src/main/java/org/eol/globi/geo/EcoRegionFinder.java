package org.eol.globi.geo;

import com.vividsolutions.jts.geom.Point;

import java.util.Map;

public interface EcoRegionFinder {
    Map<String, String> findEcoRegion(Point point) throws EcoRegionFinderException;

    Map<String, String> findEcoRegion(double lat, double lng) throws EcoRegionFinderException;

}
