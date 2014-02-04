package org.eol.globi.service;

import org.eol.globi.geo.EcoRegion;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderException;
import org.eol.globi.geo.EcoRegionFinderFactory;
import org.eol.globi.geo.EcoRegionFinderFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoRegionFinderProxy implements EcoRegionFinder {
    public static final ArrayList<EcoRegion> EMPTY_REGIONS = new ArrayList<EcoRegion>();
    public final List<EcoRegionFinder> finders;

    public EcoRegionFinderProxy(List<EcoRegionFinder> finders) {
        this.finders = finders;
    }

    @Override
    public Collection<EcoRegion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException {
        Map<String, EcoRegion> regions = null;
        for (EcoRegionFinder finder : finders) {
            Collection<EcoRegion> ecoRegion = finder.findEcoRegion(lat, lng);
            if (ecoRegion != null) {
                for (EcoRegion region : ecoRegion) {
                    if (regions == null) {
                        regions = new HashMap<String, EcoRegion>();
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
            for (EcoRegionFinder finder : finders) {
                finder.shutdown();
            }
        }
    }
}
