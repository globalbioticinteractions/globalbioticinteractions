package org.globalbioticinteractions.content;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ContentTestUtil {

    private File cacheDir;

    @Before
    public void initCache() throws IOException {
        cacheDir = new File("target/" + UUID.randomUUID());
        FileUtils.forceMkdir(cacheDir);
    }

    File getCacheDir() {
        return cacheDir;
    }

    @After
    public void cleanCache() {
        FileUtils.deleteQuietly(cacheDir);
    }


}