package org.eol.globi.geo;

import java.util.Collection;

public interface EcoRegionFinder {
    Collection<EcoRegion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException;

}
