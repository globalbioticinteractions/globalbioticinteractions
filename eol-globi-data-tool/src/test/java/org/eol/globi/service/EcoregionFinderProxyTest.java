package org.eol.globi.service;

import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EcoregionFinderProxyTest {

    @Test
    public void multipleRegionsForSingleLocation() throws EcoregionFinderException {
        ArrayList<EcoregionFinder> finders = new ArrayList<EcoregionFinder>() {
            {
                add(new EcoregionTestFinder("one:"));
                add(new EcoregionTestFinder("two:"));
                add(new EcoregionTestFinder("two:"));
                add(new EcoregionTestFinder("three:"));
            }
        };
        Collection<Ecoregion> ecoregion = new EcoregionFinderProxy(finders).findEcoregion(9.2, -79.91667);

        Collection<String> ids = new ArrayList<String>();
        for (Ecoregion region : ecoregion) {
            ids.add(region.getId());
        }

        assertThat(ids.contains("one:123"), is(true));
        assertThat(ids.contains("two:123"), is(true));
        assertThat(ids.contains("three:123"), is(true));

        assertThat(ecoregion.size(), is(3));

    }

    private class EcoregionTestFinder implements EcoregionFinder {
        private final String name;

        public EcoregionTestFinder(String name) {
            this.name = name;

        }

        @Override
        public Collection<Ecoregion> findEcoregion(double lat, double lng) throws EcoregionFinderException {
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
