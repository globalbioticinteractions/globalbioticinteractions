package org.eol.globi.service;

import org.eol.globi.data.CMECSService;
import org.eol.globi.domain.Term;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CMECSServiceTest {

    @Test
    public void lookupCMECSTerms() throws IOException, TermLookupServiceException {
        TermLookupService service = new CMECSService();

        List<Term> estuarine = service.lookupTermByName("Estuarine");
        assertThat(estuarine.size(), is(1));
        assertThat(estuarine.get(0).getName(), is("Estuarine"));
        assertThat(estuarine.get(0).getId(), is("CMECS:AQUATIC_SETTING:2"));

        List<Term> marineNearshoreSupratidal = service.lookupTermByName("Marine Nearshore Supratidal");
        assertThat(marineNearshoreSupratidal.size(), is(1));
        assertThat(marineNearshoreSupratidal.get(0).getName(), is("Marine Nearshore Supratidal"));
        assertThat(marineNearshoreSupratidal.get(0).getId(), is("CMECS:AQUATIC_SETTING:15"));

        assertThat(ExternalIdUtil.infoURLForExternalId("CMECS:AQUATIC_SETTING:15"), is("http://cmecscatalog.org/classification/aquaticSetting/15"));
    }

}
