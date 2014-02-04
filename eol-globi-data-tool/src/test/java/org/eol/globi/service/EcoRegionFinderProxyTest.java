package org.eol.globi.service;

import org.eol.globi.geo.EcoRegion;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EcoRegionFinderProxyTest {

    @Test
    public void multipleRegionsForSingleLocation() throws EcoRegionFinderException {
        ArrayList<EcoRegionFinder> finders = new ArrayList<EcoRegionFinder>() {
            {
                add(new EcoRegionTestFinder("one:"));
                add(new EcoRegionTestFinder("two:"));
                add(new EcoRegionTestFinder("two:"));
                add(new EcoRegionTestFinder("three:"));
            }
        };
        Collection<EcoRegion> ecoRegion = new EcoRegionFinderProxy(finders).findEcoRegion(9.2, -79.91667);

        Collection<String> ids = new ArrayList<String>();
        for (EcoRegion region : ecoRegion) {
            ids.add(region.getId());
        }

        assertThat(ids.contains("one:123"), is(true));
        assertThat(ids.contains("two:123"), is(true));
        assertThat(ids.contains("three:123"), is(true));

        assertThat(ecoRegion.size(), is(3));

    }

    private class EcoRegionTestFinder implements EcoRegionFinder {
        private final String name;

        public EcoRegionTestFinder(String name) {
            this.name = name;

        }

        @Override
        public Collection<EcoRegion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException {
            return new ArrayList<EcoRegion>() {
                {
                    EcoRegion ecoRegion = new EcoRegion();
                    ecoRegion.setId(name + "123");
                    add(ecoRegion);
                }
            };
        }


        @Override
        public void shutdown() {

        }
    }
}
