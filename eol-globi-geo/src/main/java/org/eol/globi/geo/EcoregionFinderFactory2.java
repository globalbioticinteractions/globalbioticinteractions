package org.eol.globi.geo;

import java.util.List;

public interface EcoregionFinderFactory2 {
    EcoregionFinder createEcoregionFinder(EcoregionType type);

    List<EcoregionFinder> createAll();
}
