package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TermLookupServiceImplTest {

    @Test
    public void localURL() throws IOException {
        URL url = getClass().getResource("some.jar");
        assertNotNull(url);
        URI uri = URI.create("jar:" + url.toString() + "!/META-INF/MANIFEST.MF");
        assertThat(uri.getScheme(), is("jar"));
        String response;
        if ("file".equals(uri.getScheme()) || "jar".equals(uri.getScheme())) {
            response = IOUtils.toString(uri.toURL(), StandardCharsets.UTF_8);
        } else {
            response = HttpUtil.getContent(uri);
        }
        String manifest = response;
        assertThat(manifest, is(notNullValue()));
    }

}