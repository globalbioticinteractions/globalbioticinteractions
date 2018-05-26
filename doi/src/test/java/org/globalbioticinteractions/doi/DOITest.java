package org.globalbioticinteractions.doi;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DOITest {

    @Test
    public void constructor() {
        DOI doi = new DOI("123", "456");
        assertThat(doi.getDirectoryIndicator(), is("10"));
        assertThat(doi.getRegistrantCode(), is("123"));
        assertThat(doi.getSuffix(), is("456"));
        assertThat(doi.toString(), is("10.123/456"));
        assertThat(doi.getPrintableDOI(), is("doi:10.123/456"));
        assertThat(doi.toURI().toString(), is("https://doi.org/10.123/456"));
    }

    @Test
    public void fromURL() throws URISyntaxException, MalformedDOIException {
        DOI.create(new URI("https://doi.org/10.123/456"));
        DOI.create("doi:10.123/456");
        DOI.create("DOI:10.123/456");
        DOI.create("DoI:10.123/456");
    }

    @Test
    public void create() throws MalformedDOIException, URISyntaxException {
        DOI.create("doi:10.12/123");
    }

    @Test
    public void doiFormat() throws URISyntaxException, MalformedDOIException {
        assertThat(DOI.create("doi:10.1234/567").toString(), is("10.1234/567"));
        assertThat(DOI.create("DOI:10.1234/567").toString(), is("10.1234/567"));
        assertThat(DOI.create("10.1676/10-092.1").toString(), is("10.1676/10-092.1"));
        assertThat(DOI.create("http://dx.doi.org/10.1676/10-092.1").toString(), is("10.1676/10-092.1"));
    }

    // see https://www.doi.org/doi_handbook/2_Numbering.html#2.2
    @Test
    public void validDOI() throws MalformedDOIException {
        DOI.create("10.1000/123456");
        DOI.create("10.1038/issn.1476-4687");
    }

    @Test(expected = MalformedDOIException.class)
    public void invalidDOIInvalidDirectoryIndicator() throws MalformedDOIException {
        DOI.create("9.1000/123456");
    }

    @Test(expected = MalformedDOIException.class)
    public void invalidDOIMissingSuffix() throws MalformedDOIException {
        DOI.create("10.1038.issn.1476-4687");
    }

    @Test(expected = MalformedDOIException.class)
    public void invalidDOIMissingRegistrant() throws MalformedDOIException {
        DOI.create("10");
    }

    @Test(expected = MalformedDOIException.class)
    public void invalidDOI() throws MalformedDOIException {
        assertThat(DOI.create("boahhhs"), is(nullValue()));
    }

    @Test
    public void toURL() throws URISyntaxException, MalformedDOIException, MalformedURLException {
        assertThat(DOI.create("10.1000/123456").toString(), is("10.1000/123456"));
        assertThat(DOI.create("10.1000/123#456").toURI().toString(), is("https://doi.org/10.1000/123%23456"));
        assertThat(DOI.create("10.1206/0003-0090(2000)264<0083:>2.0.co;2").toURI().toURL().toString(), is("https://doi.org/10.1206/0003-0090(2000)264%3C0083:%3E2.0.co;2"));
    }

    @Test
    public void toURIForResolver() throws URISyntaxException, MalformedDOIException, MalformedURLException {
        assertThat(DOI.create("10.1000/123456").toURI(URI.create("https://example.org")).toString(), is("https://example.org/10.1000/123456"));
    }


    @Test
    public void toPrintableURL() throws URISyntaxException, MalformedDOIException {
        assertThat(DOI.create("10.1000/123456").getPrintableDOI(), is("doi:10.1000/123456"));
        assertThat(DOI.create("10.1000/123456").getPrintableDOI(), is("doi:10.1000/123456"));
        assertThat(DOI.create("10.1000/123#456").getPrintableDOI(), is("doi:10.1000/123#456"));
        assertThat(DOI.create("10.1206/0003-0090(2000)264<0083:>2.0.co;2").getPrintableDOI(), is("doi:10.1206/0003-0090(2000)264<0083:>2.0.co;2"));
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
    public void fromURL2() throws MalformedDOIException {
        assertThat(DOI.create("https://doi.org/10.1000/123%23456").toString(), is("10.1000/123#456"));
        assertThat(DOI.create("https://doi.org/10.1206/0003-0090(2000)264%3C0083:%3E2.0.co;2").toString(), is("10.1206/0003-0090(2000)264<0083:>2.0.co;2"));
    }

    @Test(expected = MalformedDOIException.class)
    public void fromURL4() throws MalformedDOIException {
        String doiString = "http://dx.doi.org/10.1898/1051-1733(2004)085<0062:dcabso>2.0.co;2";
        DOI.create(doiString);
    }

    @Test
    public void escaping() throws MalformedDOIException {
        String originalDOI = "10.1898/1051-1733(2004)085<0062:dcabso>2.0.co;2";
        DOI doi = DOI.create(originalDOI);
        assertThat(doi.toString(), is(originalDOI));
        URI uri = doi.toURI(URI.create("http://dx.doi.org/"));
        assertThat(uri.toString(), is(not("http://dx.doi.org/10.1898/1051-1733(2004)085<0062:dcabso>2.0.co;2")));
        assertThat(uri.toString(), is("http://dx.doi.org/10.1898/1051-1733(2004)085%3C0062:dcabso%3E2.0.co;2"));
    }

    @Test
    public void whitespace() throws MalformedDOIException {
        DOI.create("10.some/some citation");
    }

    @Test
    public void equals() throws MalformedDOIException {
        DOI doi1 = DOI.create("https://doi.org/10.1/ABC");
        DOI doi2 = DOI.create("https://doi.org/10.1/AbC");
        DOI doi3 = DOI.create("https://doi.org/10.1/AbCD");

        assertThat(doi1, is(doi2));
        assertThat(doi2, is(not(doi3)));
        assertThat(doi1, is(not(doi3)));
    }
}