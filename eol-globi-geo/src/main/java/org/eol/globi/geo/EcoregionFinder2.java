package org.eol.globi.geo;

import java.util.Collection;

public interface EcoregionFinder2 {
    Collection<Ecoregion2> findEcoregion(double lat, double lng) throws EcoregionFinderException2;

    void shutdown();

}
