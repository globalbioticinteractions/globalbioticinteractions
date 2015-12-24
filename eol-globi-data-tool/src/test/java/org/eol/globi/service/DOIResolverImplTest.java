package org.eol.globi.service;

import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class DOIResolverImplTest {

    @Test
    public void doiFormat() throws URISyntaxException {
        assertThat(DOIResolverImpl.URIfor("doi:1234").toString(), is("http://dx.doi.org/1234"));
        assertThat(DOIResolverImpl.URIfor("DOI:1234").toString(), is("http://dx.doi.org/1234"));
        assertThat(DOIResolverImpl.URIfor("10.1676/10-092.1").toString(), is("http://dx.doi.org/10.1676/10-092.1"));
    }

}