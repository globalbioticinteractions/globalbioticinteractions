package org.eol.globi.service;

import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoRegionFinderException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoRegionFinderProxy implements EcoregionFinder {
    public static final ArrayList<Ecoregion> EMPTY_REGIONS = new ArrayList<Ecoregion>();
    public final List<EcoregionFinder> finders;

    public EcoRegionFinderProxy(List<EcoregionFinder> finders) {
        this.finders = finders;
    }

    @Override
    public Collection<Ecoregion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException {
        Map<String, Ecoregion> regions = null;
        for (EcoregionFinder finder : finders) {
            Collection<Ecoregion> ecoregion = finder.findEcoRegion(lat, lng);
            if (ecoregion != null) {
                for (Ecoregion region : ecoregion) {
                    if (regions == null) {
                        regions = new HashMap<String, Ecoregion>();
                    }
                    regions.put(region.getId(), region);
                }
            }
        }
        return regions == null ? EMPTY_REGIONS : regions.values();
    }

    @Override
    public void shutdown() {
        if (finders != null) {
            for (EcoregionFinder finder : finders) {
                finder.shutdown();
            }
        }
    }
}
