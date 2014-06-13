package org.eol.globi.geo;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Ecoregion2FinderFactory2ImplTest {

    public static final String SHAPEFILES_DIR = "shapefiles.dir";
    private String oldPropertyValue;

    @Before
    public void saveProps() {
        oldPropertyValue = System.getProperty(SHAPEFILES_DIR);

    }

    @After
    public void reloadProps() {
        if (StringUtils.isBlank(oldPropertyValue)) {
            System.clearProperty(SHAPEFILES_DIR);
        } else {
            System.setProperty(SHAPEFILES_DIR, oldPropertyValue);
        }
    }

    @Test
    public void checkDataSources() throws EcoregionFinderException2 {
        assertThat(oldPropertyValue, is(nullValue()));
        for (EcoregionType2 type : EcoregionType2.values()) {
            EcoregionFinder2 ecoregionFinder2 = new EcoregionFinderFactoryImpl2().createEcoregionFinder(type);
            ecoregionFinder2.findEcoregion(10.2, 102);
        }
    }

    @Test(expected = Exception.class)
    public void checkDataSourcesUsingSystemPropertyIncorrect() throws EcoregionFinderException2 {
        System.setProperty(SHAPEFILES_DIR, "/thisdoesnotexistatall");
        for (EcoregionType2 type : EcoregionType2.values()) {
            EcoregionFinder2 ecoregionFinder2 = new EcoregionFinderFactoryImpl2().createEcoregionFinder(type);
            ecoregionFinder2.findEcoregion(10.2, 102);
        }
    }
}
