package org.eol.globi.taxon;

import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
        String manifest = ResourceUtil.contentToString(uri);
        assertThat(manifest, is(notNullValue()));
    }

}