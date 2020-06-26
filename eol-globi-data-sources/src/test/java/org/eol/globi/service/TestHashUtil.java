package org.eol.globi.service;

import org.apache.commons.io.output.NullOutputStream;
import org.globalbioticinteractions.cache.CacheUtil;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestHashUtil {
    public static void assertContentHash(InputStream retrieve, String expectedHash) throws IOException {
        try {
            String actualHash = CacheUtil.calculateContentHash(retrieve, new NullOutputStream());
            assertThat(actualHash, is(expectedHash));
        } catch (NoSuchAlgorithmException e) {
            fail("failed to verify hashes: [" + e.getMessage() + "]");
        }
    }
}
