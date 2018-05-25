package org.globalbioticinteractions.util;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DOIUtilTest {

    @Test
    public void doiFormat() throws URISyntaxException {
        assertThat(DOIUtil.URIfor("doi:10.1234").toString(), is("https://doi.org/10.1234"));
        assertThat(DOIUtil.URIfor("DOI:10.1234").toString(), is("https://doi.org/10.1234"));
        assertThat(DOIUtil.URIfor("10.1676/10-092.1").toString(), is("https://doi.org/10.1676/10-092.1"));
        assertThat(DOIUtil.URIfor("10.1206/0003-0090(2000)264<0083:>2.0.co;2").toString(), is("https://doi.org/10.1206/0003-0090(2000)264%3C0083:%3E2.0.co;2"));
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
    public void toURL() throws URISyntaxException {
        assertThat(DOIUtil.urlForDOI("10.1000/123456"), is("https://doi.org/10.1000/123456"));
        assertThat(DOIUtil.urlForDOI("10.1000/123#456"), is("https://doi.org/10.1000/123%23456"));
    }

    @Test
    public void easyDOIEncodingMistakeToMakeWithURIClass() throws URISyntaxException {
        // e.g., from https://www.doi.org/syntax.html:
        // "Hex encoding consists of substituting for the given character its hex value preceded by percent. Thus, # becomes %23 and
        // http://dx.doi.org/10.1000/456#789
        // is encoded as
        // http://dx.doi.org/10.1000/456%23789
        // a DOI util is needed to avoid"

        URI actual = new URI("https://resolv.org/10.1000/123#456");
        assertThat(actual.getPath(), is("/10.1000/123"));
        assertThat(actual.getFragment(), is("456"));
        assertThat(actual, is(not(URI.create("https://resolv.org/10.1000/123%23456"))));
        // but
        assertThat(new URI("https", "resolv.org", "/10.1000/123#456", null), is(URI.create("https://resolv.org/10.1000/123%23456")));
    }

    @Test
    public void fromURL() {
        assertThat(DOIUtil.stripDOIPrefix("https://doi.org/10.1000/123%23456"), is("10.1000/123#456"));
        assertThat(DOIUtil.stripDOIPrefix("https://doi.org/10.1206/0003-0090(2000)264%3C0083:%3E2.0.co;2"), is("10.1206/0003-0090(2000)264<0083:>2.0.co;2"));
    }

}