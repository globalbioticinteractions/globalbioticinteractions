package org.eol.globi.geo;

import org.junit.Test;

public class EcoRegionFinderFactoryTest {

    @Test
    public void checkDataSources() throws EcoRegionFinderException {
        for (EcoRegionType ecoRegionType : EcoRegionType.values()) {
            EcoRegionFinder ecoRegionFinder = new EcoRegionFinderFactory().createEcoRegionFinder(ecoRegionType);
            ecoRegionFinder.findEcoRegion(10.2, 102);
        }
    }
}
