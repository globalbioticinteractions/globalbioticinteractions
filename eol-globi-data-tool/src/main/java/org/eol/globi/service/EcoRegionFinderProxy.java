package org.eol.globi.service;

import org.eol.globi.geo.EcoRegion;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderException;
import org.eol.globi.geo.EcoRegionFinderFactory;
import org.eol.globi.geo.EcoRegionFinderFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EcoRegionFinderProxy implements EcoRegionFinder {
    private final EcoRegionFinderFactory factory;
    public List<EcoRegionFinder> finders;

    public EcoRegionFinderProxy(EcoRegionFinderFactory factory) {
        this.factory = factory;
    }

    @Override
    public Collection<EcoRegion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException {
        if (finders == null) {
            finders = factory.createAll();
        }
        Collection<EcoRegion> regions = null;
        for (EcoRegionFinder finder : finders) {
            Collection<EcoRegion> ecoRegion = finder.findEcoRegion(lat, lng);
            if (ecoRegion.size() > 0 && regions == null) {
                regions = new ArrayList<EcoRegion>();
                for (EcoRegion region : ecoRegion) {
                    regions.add(region);
                }
            }
        }
        return regions;
    }
}
