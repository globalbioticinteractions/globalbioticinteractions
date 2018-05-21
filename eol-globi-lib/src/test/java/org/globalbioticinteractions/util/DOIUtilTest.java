package org.globalbioticinteractions.util;

import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DOIUtilTest {

    @Test
    public void doiFormat() throws URISyntaxException {
        assertThat(DOIUtil.URIfor("doi:10.1234").toString(), is("https://doi.org/10.1234"));
        assertThat(DOIUtil.URIfor("DOI:10.1234").toString(), is("https://doi.org/10.1234"));
        assertThat(DOIUtil.URIfor("10.1676/10-092.1").toString(), is("https://doi.org/10.1676/10-092.1"));
        assertThat(DOIUtil.URIfor("http://dx.doi.org/10.1676/10-092.1").toString(), is("https://doi.org/10.1676/10-092.1"));
        assertThat(DOIUtil.URIfor("boahhhs"), is(nullValue()));
    }

    // see https://www.doi.org/doi_handbook/2_Numbering.html#2.2
    @Test
    public void validDOI() {
        assertTrue(DOIUtil.isValid("10.1000/123456"));
        assertTrue(DOIUtil.isValid("10.1038/issn.1476-4687"));
    }

    @Test
    public void invalidDOI() {
        assertFalse(DOIUtil.isValid("9.1000/123456"));
        assertFalse(DOIUtil.isValid("9.1038/issn.1476-4687"));
    }

    @Test
    public void toURL() {
        assertThat(DOIUtil.urlForDOI("10.1000/123456"), is("https://doi.org/10.1000/123456"));
        assertThat(DOIUtil.urlForDOI("10.1000/123#456"), is("https://doi.org/10.1000/123%23456"));
    }

   @Test
    public void fromURL() {
        assertThat(DOIUtil.stripDOIPrefix("https://doi.org/10.1000/123%23456"), is("10.1000/123#456"));
    }

}