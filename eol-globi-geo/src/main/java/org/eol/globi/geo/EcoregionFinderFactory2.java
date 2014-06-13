package org.eol.globi.geo;

import java.util.List;

public interface EcoregionFinderFactory2 {
    EcoregionFinder2 createEcoregionFinder(EcoregionType2 type);

    List<EcoregionFinder2> createAll();
}
