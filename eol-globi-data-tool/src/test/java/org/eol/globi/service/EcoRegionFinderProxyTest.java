package org.eol.globi.service;

import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoRegionFinderException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EcoregionFinderProxyTest {

    @Test
    public void multipleRegionsForSingleLocation() throws EcoRegionFinderException {
        ArrayList<EcoregionFinder> finders = new ArrayList<EcoregionFinder>() {
            {
                add(new EcoRegionTestFinder("one:"));
                add(new EcoRegionTestFinder("two:"));
                add(new EcoRegionTestFinder("two:"));
                add(new EcoRegionTestFinder("three:"));
            }
        };
        Collection<Ecoregion> ecoregion = new EcoRegionFinderProxy(finders).findEcoRegion(9.2, -79.91667);

        Collection<String> ids = new ArrayList<String>();
        for (Ecoregion region : ecoregion) {
            ids.add(region.getId());
        }

        assertThat(ids.contains("one:123"), is(true));
        assertThat(ids.contains("two:123"), is(true));
        assertThat(ids.contains("three:123"), is(true));

        assertThat(ecoregion.size(), is(3));

    }

    private class EcoRegionTestFinder implements EcoregionFinder {
        private final String name;

        public EcoRegionTestFinder(String name) {
            this.name = name;

        }

        @Override
        public Collection<Ecoregion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException {
            return new ArrayList<Ecoregion>() {
                {
                    Ecoregion ecoregion = new Ecoregion();
                    ecoregion.setId(name + "123");
                    add(ecoregion);
                }
            };
        }


        @Override
        public void shutdown() {

        }
    }
}
