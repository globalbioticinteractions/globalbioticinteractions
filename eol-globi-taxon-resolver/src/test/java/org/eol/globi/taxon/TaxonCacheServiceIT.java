package org.eol.globi.taxon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class TaxonCacheServiceIT {

    private File mapdbDir;

    @Before
    public void createMapDBFolder() throws IOException {
        mapdbDir = new File("./target/mapdb" + new Random().nextLong());
    }

    @After
    public void deleteMapDBFolder() throws IOException {
        FileUtils.deleteQuietly(mapdbDir);
    }

    @Test
    public void init10k() throws PropertyEnricherException {
        final TaxonCacheService cacheService = new TaxonCacheService("/org/eol/globi/taxon/taxonCache10k.tsv.gz", "/org/eol/globi/taxon/taxonMap10k.tsv.gz");
        cacheService.setCacheDir(mapdbDir);
        StopWatch watch = new StopWatch();
        final TaxonImpl taxon = new TaxonImpl();
        taxon.setExternalId("EOL:1049789");

        watch.start();
        final Map<String, String> enriched = cacheService.enrich(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Animalia | Chordata | Aves | Columbiformes | Columbidae | Turtur | Turtur tympanistria"));
        watch.stop();
    }
}