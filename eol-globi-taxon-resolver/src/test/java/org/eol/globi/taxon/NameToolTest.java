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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class NameToolTest {

    @Test
    public void resolve() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricherPassThrough enricher = new PropertyEnricherPassThrough();
        NameTool.resolve(is, new NameTool.ResolvingRowHandler(enricher, false, os));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tSAME_AS\tNCBI:9606\tHomo sapiens\t\t\t\t\t\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606\t\tA name source\thttp://example.org\t1970-01-01T00:00:00Z\n"));
    }

    @Test
    public void resolveAppend() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricherPassThrough enricher = new PropertyEnricherPassThrough();
        NameTool.resolve(is, new NameTool.ResolvingRowHandler(enricher, false, os));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tone\tSAME_AS\tNCBI:9606\tHomo sapiens\t\t\t\t\t\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606\t\tA name source\thttp://example.org\t1970-01-01T00:00:00Z\n"));
    }

    @Test
    public void resolveAppendFuzzyMatch() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tHomo saliens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.GlobalNamesRowHandler(false, os));
        assertThat(os.toString(), containsString("\tHomo saliens\tone\tSIMILAR_TO\t"));
    }

    @Test
    public void resolveReplace() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricherPassThrough enricher = new PropertyEnricherPassThrough();
        NameTool.resolve(is, new NameTool.ResolvingRowHandler(enricher, true, os));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tone\n"));
    }

    @Test
    public void resolveGlobalNamesBatch() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.GlobalNamesRowHandler(true, os));
        assertThat(os.toString(), containsString("NCBI:9606\tHomo sapiens\tone\n"));
    }

    @Test
    public void resolveGlobalNamesBatchAppend() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.GlobalNamesRowHandler(false, os));
        assertThat(os.toString(), containsString("Mammalia"));
        assertThat(os.toString(), containsString("nih.gov"));
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
