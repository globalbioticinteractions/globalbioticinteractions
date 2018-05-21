package org.eol.globi.service;

import org.globalbioticinteractions.util.DOIUtil;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class DOIResolverImplTest {

    @Test
    public void doiFormat() throws URISyntaxException {
        assertThat(DOIUtil.URIfor("doi:1234").toString(), is("https://doi.org/1234"));
        assertThat(DOIUtil.URIfor("DOI:1234").toString(), is("https://doi.org/1234"));
        assertThat(DOIUtil.URIfor("10.1676/10-092.1").toString(), is("https://doi.org/10.1676/10-092.1"));
        assertThat(DOIUtil.URIfor("http://dx.doi.org/10.1676/10-092.1").toString(), is("https://doi.org/10.1676/10-092.1"));
    }

}