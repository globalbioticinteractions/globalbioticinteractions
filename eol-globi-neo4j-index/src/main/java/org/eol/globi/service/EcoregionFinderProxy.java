package org.eol.globi.service;

import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoregionFinderProxy implements EcoregionFinder {
    public static final ArrayList<Ecoregion> EMPTY_REGIONS = new ArrayList<Ecoregion>();
    public final List<EcoregionFinder> finders;

    public EcoregionFinderProxy(List<EcoregionFinder> finders) {
        this.finders = finders;
    }

    @Override
    public Collection<Ecoregion> findEcoregion(double lat, double lng) throws EcoregionFinderException {
        Map<String, Ecoregion> regions = null;
        for (EcoregionFinder finder : finders) {
            Collection<Ecoregion> ecoregion = finder.findEcoregion(lat, lng);
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
