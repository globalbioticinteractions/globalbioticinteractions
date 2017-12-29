package org.eol.globi.taxon;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.DateUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertThat;

public class NameToolTest {

    @Test
    public void resolve() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, os, false, new PropertyEnricherPassThrough());
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tNCBI:9606\tHomo sapiens\t\t\t\t\t\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606\t\tA name source\thttp://example.org\t1970-01-01T00:00:00Z\n"));
    }

    @Test
    public void resolveAppend() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, os, false, new PropertyEnricherPassThrough());
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tone\tNCBI:9606\tHomo sapiens\t\t\t\t\t\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606\t\tA name source\thttp://example.org\t1970-01-01T00:00:00Z\n"));
    }

    @Test
    public void resolveReplace() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, os, true,  new PropertyEnricherPassThrough());
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tone\n"));
    }

    private static class PropertyEnricherPassThrough implements PropertyEnricher {

        @Override
        public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
            return MapUtils.unmodifiableMap(new TreeMap<String, String>(properties) {{
                put(PropertyAndValueDictionary.NAME_SOURCE, "A name source");
                put(PropertyAndValueDictionary.NAME_SOURCE_URL, "http://example.org");
                put(PropertyAndValueDictionary.NAME_SOURCE_ACCESSED_AT, DateUtil.printDate(new Date(0)));
            }});
        }

        @Override
        public void shutdown() {

        }
    }
}
