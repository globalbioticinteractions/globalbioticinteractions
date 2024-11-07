package org.globalbioticinteractions.cache;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;

public class ContentPathDepth0Test {

    @Test
    public void uriForSha256() {
        URI bla = new ContentPathDepth0(new File("/foo/bar")).forContentId("1234");

        assertThat(bla.toString(), Is.is("file:/foo/bar/1234"));
    }

}