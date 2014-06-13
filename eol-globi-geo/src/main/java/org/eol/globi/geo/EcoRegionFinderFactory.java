package org.eol.globi.geo;

import java.util.List;

public interface EcoregionFinderFactory {
    EcoregionFinder createEcoregionFinder(EcoregionType type);

    List<EcoregionFinder> createAll();
}
