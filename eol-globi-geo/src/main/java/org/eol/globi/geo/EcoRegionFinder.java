package org.eol.globi.geo;

import java.util.Collection;

public interface EcoregionFinder {
    Collection<Ecoregion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException;

    void shutdown();

}
