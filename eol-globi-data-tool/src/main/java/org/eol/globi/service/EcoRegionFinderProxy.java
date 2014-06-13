package org.eol.globi.service;

import org.eol.globi.geo.Ecoregion2;
import org.eol.globi.geo.EcoregionFinder2;
import org.eol.globi.geo.EcoregionFinderException2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoregionFinderProxy implements EcoregionFinder2 {
    public static final ArrayList<Ecoregion2> EMPTY_REGIONS = new ArrayList<Ecoregion2>();
    public final List<EcoregionFinder2> finders;

    public EcoregionFinderProxy(List<EcoregionFinder2> finders) {
        this.finders = finders;
    }

    @Override
    public Collection<Ecoregion2> findEcoregion(double lat, double lng) throws EcoregionFinderException2 {
        Map<String, Ecoregion2> regions = null;
        for (EcoregionFinder2 finder : finders) {
            Collection<Ecoregion2> ecoregion2 = finder.findEcoregion(lat, lng);
            if (ecoregion2 != null) {
                for (Ecoregion2 region : ecoregion2) {
                    if (regions == null) {
                        regions = new HashMap<String, Ecoregion2>();
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
            for (EcoregionFinder2 finder : finders) {
                finder.shutdown();
            }
        }
    }
}
