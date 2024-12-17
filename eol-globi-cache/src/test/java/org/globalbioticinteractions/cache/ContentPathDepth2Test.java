package org.globalbioticinteractions.cache;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;

public class ContentPathDepth2Test {

    @Test
    public void uriForSha256() {
        URI path = new ContentPathDepth2(new File("/foo/bar")).forContentId("1234");
        assertThat(path.toString(), Is.is("file:/foo/bar/12/34/1234"));
    }

    @Test
    public void uriForSha256WithNamespace() {
        URI path = new ContentPathDepth2(new File("/foo/bar"), "some/namespace").forContentId("1234");
        assertThat(path.toString(), Is.is("file:/foo/bar/some/namespace/12/34/1234"));
    }

    @Test
    public void uriForSha256WithEmptyNamespace() {
        URI path = new ContentPathDepth2(new File("/foo/bar"), "").forContentId("1234");
        assertThat(path.toString(), Is.is("file:/foo/bar/12/34/1234"));
    }

    @Test
    public void uriForSha256WithNullNamespace() {
        URI path = new ContentPathDepth2(
                new File("/foo/bar"),
                null
        ).forContentId("1234");

        assertThat(path.toString(), Is.is("file:/foo/bar/12/34/1234"));
    }


}