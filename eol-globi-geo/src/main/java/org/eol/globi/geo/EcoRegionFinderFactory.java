package org.eol.globi.geo;

import java.util.List;

public interface EcoRegionFinderFactory {
    EcoRegionFinder createEcoRegionFinder(EcoRegionType type);

    List<EcoRegionFinder> createAll();
}
