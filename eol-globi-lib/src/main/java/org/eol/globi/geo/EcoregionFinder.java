package org.eol.globi.geo;

import java.util.Collection;

public interface EcoregionFinder {
    Collection<Ecoregion> findEcoregion(double lat, double lng) throws EcoregionFinderException;

    void shutdown();

}
