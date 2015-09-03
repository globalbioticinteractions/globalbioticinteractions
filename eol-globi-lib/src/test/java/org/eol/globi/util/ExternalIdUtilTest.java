package org.eol.globi.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExternalIdUtilTest {

    @Test
    public void mapping() {
        assertThat(ExternalIdUtil.urlForExternalId("http://blabla"), is("http://blabla"));
        assertThat(ExternalIdUtil.urlForExternalId("doi:someDOI"), is("http://dx.doi.org/someDOI"));
        assertThat(ExternalIdUtil.urlForExternalId("ENVO:00001995"), is("http://purl.obolibrary.org/obo/ENVO_00001995"));
        assertThat(ExternalIdUtil.urlForExternalId("bioinfo:ref:147884"), is("http://bioinfo.org.uk/html/b147884.htm"));
        assertThat(ExternalIdUtil.urlForExternalId("IF:700605"), is("http://www.indexfungorum.org/names/NamesRecord.asp?RecordID=700605"));
        assertThat(ExternalIdUtil.urlForExternalId("NCBI:7215"), is("https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=7215"));
    }

    @Test
    public void getExternalId() {
        assertThat(ExternalIdUtil.getUrlFromExternalId("{ \"data\": [[]]}"), is("{}"));
        assertThat(ExternalIdUtil.getUrlFromExternalId("{ \"data\": []}"), is("{}"));
        assertThat(ExternalIdUtil.getUrlFromExternalId("{}"), is("{}"));
    }

    @Test
    public void buildCitation() {
        assertThat(ExternalIdUtil.toCitation("Joe Smith", "my study", "1984"), is("Joe Smith. 1984. my study"));
        assertThat(ExternalIdUtil.toCitation("Joe Smith", null, "1984"), is("Joe Smith. 1984"));
        assertThat(ExternalIdUtil.toCitation("Joe Smith", "my study", null), is("Joe Smith. my study"));
    }
}
