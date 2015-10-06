package org.eol.globi.taxon;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class TermLookupServiceImplTest {

    @Test
    public void localURL() throws URISyntaxException, IOException {
        URL url = getClass().getResource("/META-INF/MANIFEST.MF");
        URI uri = url.toURI();
        assertThat(uri.getScheme(), is("jar"));
        String manifest = TermLookupServiceImpl.contentToString(uri);
        assertThat(manifest, is(notNullValue()));
    }

}