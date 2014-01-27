package org.eol.globi.geo;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EcoRegionFinderFactoryImplTest {

    public static final String SHAPEFILES_DIR = "shapefiles.dir";
    private String oldPropertyValue;

    @Before
    public void saveProps() {
        oldPropertyValue = System.getProperty(SHAPEFILES_DIR);

    }

    @After
    public void reloadProps() {
        if (StringUtils.isNotBlank(oldPropertyValue)) {
            System.setProperty(SHAPEFILES_DIR, oldPropertyValue);
        }
    }

    @Test
    public void checkDataSources() throws EcoRegionFinderException {
        assertThat(oldPropertyValue, is(nullValue()));
        for (EcoRegionType ecoRegionType : EcoRegionType.values()) {
            EcoRegionFinder ecoRegionFinder = new EcoRegionFinderFactoryImpl().createEcoRegionFinder(ecoRegionType);
            ecoRegionFinder.findEcoRegion(10.2, 102);
        }
    }

    @Test(expected = RuntimeException.class)
    public void checkDataSourcesUsingSystemPropertyIncorrect() throws EcoRegionFinderException {
        System.setProperty(SHAPEFILES_DIR, "/thisdoesnotexistatall");
        for (EcoRegionType ecoRegionType : EcoRegionType.values()) {
            EcoRegionFinder ecoRegionFinder = new EcoRegionFinderFactoryImpl().createEcoRegionFinder(ecoRegionType);
            ecoRegionFinder.findEcoRegion(10.2, 102);
        }
    }
}
