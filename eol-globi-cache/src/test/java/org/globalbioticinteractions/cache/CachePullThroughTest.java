package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class CachePullThroughTest {

    @Rule

    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void cache() throws IOException {
        String namespace = "some/namespace";
        File dataDir = CacheUtil.findOrMakeProvOrDataDirForNamespace(folder.newFolder("datasets"), namespace);
        ContentProvenance contentProvenance
                = CachePullThrough.cache(
                URI.create("https://github.com/globalbioticinteractions/template-dataset/archive/main.zip"),
                dataDir,
                new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), folder.newFolder()),
                new ContentPathFactoryDepth0(),
                namespace
        );
        File cachedFile = new File(contentProvenance.getLocalURI());
        assertThat(cachedFile.exists(), Is.is(true));
        assertThat(cachedFile.toURI().toString(), startsWith("file:/"));
    }

    @Test
    public void retrieve() throws IOException {
        String namespace = "some/namespace";
        File dataDir = folder.newFolder("data");
        File provDir = folder.newFolder("prov");
        CachePullThrough cache = new CachePullThrough(
                namespace,
                new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), folder.newFolder()),
                new ContentPathFactoryDepth0(),
                dataDir.getAbsolutePath(),
                provDir.getAbsolutePath());

        InputStream is
                = cache.retrieve(URI.create("https://github.com/globalbioticinteractions/template-dataset/archive/main.zip"));


        IOUtils.copy(is, NullOutputStream.NULL_OUTPUT_STREAM);

        File provRecords = new File(provDir.getAbsolutePath() + "/" + namespace, "access.tsv");
        assertThat(provRecords.exists(), Is.is(true));

        List<String> lines = IOUtils.readLines(new FileInputStream(provRecords), StandardCharsets.UTF_8);

        String contentId = lines.get(0).split("\t")[2];
        assertThat(contentId.length(), Is.is(64));

        File dataFile = new File(dataDir.getAbsolutePath() + "/" + namespace, contentId);
        assertThat(dataFile.exists(), Is.is(true));


    }


}