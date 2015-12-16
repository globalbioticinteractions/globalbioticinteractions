package org.eol.globi.geo;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ResourceUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EcoregionFinderFactoryImplTest {

    private String oldPropertyValue;

    @Before
    public void saveProps() {
        oldPropertyValue = System.getProperty(ResourceUtil.SHAPEFILES_DIR);

    }

    @After
    public void reloadProps() {
        if (StringUtils.isBlank(oldPropertyValue)) {
            System.clearProperty(ResourceUtil.SHAPEFILES_DIR);
        } else {
            System.setProperty(ResourceUtil.SHAPEFILES_DIR, oldPropertyValue);
        }
    }

    @Test
    public void checkDataSources() throws EcoregionFinderException {
        assertThat(oldPropertyValue, is(nullValue()));
        for (EcoregionType type : EcoregionType.values()) {
            EcoregionFinder ecoregionFinder = new EcoregionFinderFactoryImpl().createEcoregionFinder(type);
            ecoregionFinder.findEcoregion(10.2, 102);
        }
    }

    @Test(expected = Exception.class)
    public void checkDataSourcesUsingSystemPropertyIncorrect() throws EcoregionFinderException {
        System.setProperty(ResourceUtil.SHAPEFILES_DIR, "/thisdoesnotexistatall");
        for (EcoregionType type : EcoregionType.values()) {
            EcoregionFinder ecoregionFinder = new EcoregionFinderFactoryImpl().createEcoregionFinder(type);
            ecoregionFinder.findEcoregion(10.2, 102);
        }
    }
}
