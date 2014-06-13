package org.eol.globi.service;

import org.eol.globi.geo.Ecoregion2;
import org.eol.globi.geo.EcoregionFinder2;
import org.eol.globi.geo.EcoregionFinderException2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class Ecoregion2FinderProxyTest {

    @Test
    public void multipleRegionsForSingleLocation() throws EcoregionFinderException2 {
        ArrayList<EcoregionFinder2> finders = new ArrayList<EcoregionFinder2>() {
            {
                add(new EcoregionTestFinder("one:"));
                add(new EcoregionTestFinder("two:"));
                add(new EcoregionTestFinder("two:"));
                add(new EcoregionTestFinder("three:"));
            }
        };
        Collection<Ecoregion2> ecoregion2 = new EcoregionFinderProxy(finders).findEcoregion(9.2, -79.91667);

        Collection<String> ids = new ArrayList<String>();
        for (Ecoregion2 region : ecoregion2) {
            ids.add(region.getId());
        }

        assertThat(ids.contains("one:123"), is(true));
        assertThat(ids.contains("two:123"), is(true));
        assertThat(ids.contains("three:123"), is(true));

        assertThat(ecoregion2.size(), is(3));

    }

    private class EcoregionTestFinder implements EcoregionFinder2 {
        private final String name;

        public EcoregionTestFinder(String name) {
            this.name = name;

        }

        @Override
        public Collection<Ecoregion2> findEcoregion(double lat, double lng) throws EcoregionFinderException2 {
            return new ArrayList<Ecoregion2>() {
                {
                    Ecoregion2 ecoregion2 = new Ecoregion2();
                    ecoregion2.setId(name + "123");
                    add(ecoregion2);
                }
            };
        }


        @Override
        public void shutdown() {

        }
    }
}
