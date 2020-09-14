package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CMECSService;
import org.eol.globi.domain.Term;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CMECSServiceTest {

    @Test
    public void lookupCMECSTerms() throws TermLookupServiceException {
        TermLookupService service = getService();

        List<Term> estuarine = service.lookupTermByName("Estuarine");
        assertThat(estuarine.size(), is(1));
        assertThat(estuarine.get(0).getName(), is("Estuarine"));
        assertThat(estuarine.get(0).getId(), is("https://cmecscatalog.org/cmecs/classification/aquaticSetting/2"));

        List<Term> marineNearshoreSupratidal = service.lookupTermByName("Marine Nearshore Supratidal");
        assertThat(marineNearshoreSupratidal.size(), is(1));
        assertThat(marineNearshoreSupratidal.get(0).getName(), is("Marine Nearshore Supratidal"));
        assertThat(marineNearshoreSupratidal.get(0).getId(), is("https://cmecscatalog.org/cmecs/classification/aquaticSetting/15"));

        assertThat(ExternalIdUtil.urlForExternalId("https://cmecscatalog.org/cmecs/classification/aquaticSetting/15"), is("https://cmecscatalog.org/cmecs/classification/aquaticSetting/15"));
    }

    public CMECSService getService() {
        return new CMECSService(new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                if (!StringUtils.endsWith(resourceName.toString(), "cmecs4.accdb")) {
                    throw new IOException("unexpected resource [" + resourceName + "]");
                } else {
                    try {
                        return ResourceUtil.asInputStream(getClass().getResource("/org/eol/globi/service/cmecs4.accdb").toURI(), in -> in);
                    } catch (URISyntaxException e) {
                        throw new IOException("unexpected error", e);
                    }
                }
            }

        });
    }

    @Test

    public void lookupStripsCaseInsensitive() throws TermLookupServiceException {
        TermLookupService service = getService();
        List<Term> estuarine = service.lookupTermByName("lacustrine Littoral ");
        assertThat(estuarine.size(), is(1));
        assertThat(estuarine.get(0).getName(), is("Lacustrine Littoral"));
        assertThat(estuarine.get(0).getId(), is("https://cmecscatalog.org/cmecs/classification/aquaticSetting/12"));
    }

}
