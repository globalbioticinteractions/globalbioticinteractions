package org.globalbioticinteractions.dataset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentPathDepth0;
import org.globalbioticinteractions.cache.ContentPathFactoryDepth0;
import org.globalbioticinteractions.cache.HashCalculatorSHA256;
import org.globalbioticinteractions.cache.ProvenancePathFactoryImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;

public class DatasetRegistryLocalTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    private DatasetRegistry createDatasetRegistry() throws URISyntaxException {
        URL accessFile = getClass().getResource("/test-cache/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(accessFile);
        File cacheDir = new File(accessFile.toURI()).getParentFile().getParentFile().getParentFile();
        String dataDir = cacheDir.getAbsolutePath();
        String provDir = cacheDir.getAbsolutePath();
        return new DatasetRegistryLocal(cacheDir.getAbsolutePath(),
                dataset -> {
                    try {
                        return CacheUtil.cacheFor(
                                dataset.getNamespace(),
                                dataDir,
                                provDir,
                                getService(),
                                new ResourceServiceLocal(new InputStreamFactoryNoop()),
                                new ContentPathFactoryDepth0(),
                                new ProvenancePathFactoryImpl(), new HashCalculatorSHA256()
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, new ResourceServiceLocal(new InputStreamFactoryNoop()));
    }

    private ResourceServiceLocalAndRemote getService() throws IOException {
        return new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), folder.newFolder());
    }

    @Test
    public void findNamespaces() throws DatasetRegistryException, URISyntaxException {
        assertThat(createDatasetRegistry().findNamespaces(),
                hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void dataset() throws DatasetRegistryException, URISyntaxException {
        Dataset actual = createDatasetRegistry().datasetFor("globalbioticinteractions/template-dataset");
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getConfigURI(), is(URI.create("/globi.json")));
        assertThat(actual.getArchiveURI().toString(), is("https://github.com/globalbioticinteractions/template-dataset/archive/8abd2ba18457288f33527193299504015fae6def.zip"));
        assertThat(actual.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature."));
    }

    @Test
    public void datasetLocal() throws DatasetRegistryException, URISyntaxException, IOException, NoSuchAlgorithmException {
        Path testCacheDir = Files.createTempDirectory(Paths.get("target/"), "test");
        File localDatasetDir = new File(getClass().getResource("/test-dataset-local/globi.json").toURI()).getParentFile();
        File accessFile = createLocalCacheDir(testCacheDir, localDatasetDir);

        File cacheDir = new File(accessFile.toURI()).getParentFile().getParentFile();
        DatasetRegistry registry = new DatasetRegistryLocal(cacheDir.getAbsolutePath(),
                dataset -> {
                    try {
                        return CacheUtil.cacheFor(
                                dataset.getNamespace(),
                                cacheDir.getAbsolutePath(),
                                cacheDir.getAbsolutePath(),
                                getService(),
                                new ResourceServiceLocal(new InputStreamFactoryNoop()),
                                new ContentPathFactoryDepth0(),
                                new ProvenancePathFactoryImpl(),
                                new HashCalculatorSHA256()
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, new ResourceServiceLocal(new InputStreamFactoryNoop()));


        Dataset actual = registry.datasetFor("local");
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getConfigURI(), is(URI.create("/globi.json")));
        assertThat(actual.getArchiveURI().toString(), is(localDatasetDir.toURI().toString()));
        assertThat(actual.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature."));

        CacheLocalReadonly readOnlyCache = new CacheLocalReadonly(
                "local",
                cacheDir.getAbsolutePath(),
                cacheDir.getAbsolutePath(), new ResourceServiceLocal(new InputStreamFactoryNoop()),
                new ContentPathFactoryDepth0(),
                new ProvenancePathFactoryImpl()
        );
        InputStream inputStream = readOnlyCache.retrieve(URI.create("https://example.org/data.zip"));

        String actualHash = new HashCalculatorSHA256().calculateContentHash(inputStream, NullOutputStream.NULL_OUTPUT_STREAM);
        assertThat(actualHash, is("6bfc17b8717e6e8e478552f12404bc8887d691a155ffd9cd9bfc80cb6747c5d2"));
    }

    @Test(expected = DatasetRegistryException.class)
    public void datasetLocalNoValidAccessFile() throws DatasetRegistryException, URISyntaxException, IOException {
        Path testCacheDir = Files.createTempDirectory(Paths.get("target/"), "test");
        File localDatasetDir = new File(getClass().getResource("/test-dataset-local/globi.json").toURI()).getParentFile();
        String namespace = "local";
        File testCacheDirLocal = new File(testCacheDir.toFile(), namespace);
        FileUtils.forceMkdir(testCacheDirLocal);
        File accessFile1 = createInvalidProvenanceLog(localDatasetDir, testCacheDirLocal);
        addCachedResources(testCacheDir.toFile(), namespace);

        File cacheDir = new File(accessFile1.toURI()).getParentFile().getParentFile();
        DatasetRegistry registry = new DatasetRegistryLocal(cacheDir.getAbsolutePath(),
                dataset -> {
                    try {
                        return CacheUtil.cacheFor(
                                dataset.getNamespace(),
                                cacheDir.getAbsolutePath(),
                                cacheDir.getAbsolutePath(),
                                getService(),
                                new ResourceServiceLocal(new InputStreamFactoryNoop()),
                                new ContentPathFactoryDepth0(),
                                new ProvenancePathFactoryImpl(), new HashCalculatorSHA256()
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, new ResourceServiceLocal(new InputStreamFactoryNoop()));

        Iterable<String> availableNamespaces = registry.findNamespaces();
        for (String availableNamespace : availableNamespaces) {
            assertThat(availableNamespace, is(not(namespace)));
        }

        try {
            registry.datasetFor(namespace);
        } catch (DatasetRegistryException ex) {
            assertThat(ex.getMessage(), is("failed to retrieve/cache dataset in namespace [local]"));
            throw ex;
        }

    }

    public File createLocalCacheDir(Path testCacheDir, File localDatasetDir) throws IOException {
        String namespace = "local";
        File testCacheDirLocal = new File(testCacheDir.toFile(), namespace);
        FileUtils.forceMkdir(testCacheDirLocal);
        File accessFile = createProvenanceLog(localDatasetDir, testCacheDirLocal);
        addCachedResources(testCacheDir.toFile(), namespace);
        return accessFile;
    }

    public void addCachedResources(File testCacheDir, String namespace) throws IOException {
        String sha256 = "6bfc17b8717e6e8e478552f12404bc8887d691a155ffd9cd9bfc80cb6747c5d2";
        InputStream resourceAsStream = getClass().getResourceAsStream("/org/globalbioticinteractions/dataset/" + sha256);
        assertNotNull(resourceAsStream);

        FileUtils.copyInputStreamToFile(
                resourceAsStream,
                new File(new ContentPathDepth0(testCacheDir, namespace).forContentId(sha256))
        );
    }

    public File createProvenanceLog(File localDatasetDir, File testCacheDirLocal) throws IOException {
        File accessFile = new File(testCacheDirLocal, "access.tsv");
        String provenanceEntry = "local\t" + localDatasetDir.toURI().toString() + "\t\t2017-09-15T16:45:38Z\tapplication/globi" +
                "\nlocal\thttps://example.org/data.zip\t6bfc17b8717e6e8e478552f12404bc8887d691a155ffd9cd9bfc80cb6747c5d2\t2017-09-15T16:45:38Z\n";
        FileUtils.write(accessFile, provenanceEntry, StandardCharsets.UTF_8);
        assertNotNull(accessFile);
        return accessFile;
    }

    public File createInvalidProvenanceLog(File localDatasetDir, File testCacheDirLocal) throws IOException {
        File accessFile = new File(testCacheDirLocal, "access.tsv");
        String provenanceEntry =
                "local\thttps://example.org/data.zip\t6bfc17b8717e6e8e478552f12404bc8887d691a155ffd9cd9bfc80cb6747c5d2\t2017-09-15T16:45:38Z\n";
        FileUtils.write(accessFile, provenanceEntry, StandardCharsets.UTF_8);
        assertNotNull(accessFile);
        return accessFile;
    }


    @Test(expected = DatasetRegistryException.class)
    public void nonExistingDataset() throws DatasetRegistryException, URISyntaxException {
        createDatasetRegistry().datasetFor("non/existing");
    }

}