package org.globalbioticinteractions.dataset;

import org.apache.commons.io.FileUtils;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.cache.CacheUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class DatasetRegistryLocalTest {

    private DatasetRegistry createDatasetRegistry() throws URISyntaxException {
        URL accessFile = getClass().getResource("/test-cache/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(accessFile);
        File cacheDir = new File(accessFile.toURI()).getParentFile().getParentFile().getParentFile();
        return new DatasetRegistryLocal(cacheDir.getAbsolutePath(),
                dataset -> CacheUtil.cacheFor(
                        dataset.getNamespace(),
                        cacheDir.getAbsolutePath(),
                        inStream -> inStream));
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
        assertThat(actual.getConfigURI().toString(), endsWith("/test-cache/globalbioticinteractions/template-dataset/6bfc17b8717e6e8e478552f12404bc8887d691a155ffd9cd9bfc80cb6747c5d2!/template-dataset-8abd2ba18457288f33527193299504015fae6def/globi.json"));
        assertThat(actual.getArchiveURI().toString(), is("https://github.com/globalbioticinteractions/template-dataset/archive/8abd2ba18457288f33527193299504015fae6def.zip"));
        assertThat(actual.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature."));
    }

    @Test
    public void datasetLocal() throws DatasetRegistryException, URISyntaxException, IOException {
        Path testCacheDir = Files.createTempDirectory(Paths.get("target/"), "test");
        File localDatasetDir = new File(getClass().getResource("/test-dataset-local/globi.json").toURI()).getParentFile();
        File accessFile = createLocalCacheDir(testCacheDir, localDatasetDir);

        File cacheDir = new File(accessFile.toURI()).getParentFile().getParentFile();
        DatasetRegistry registry = new DatasetRegistryLocal(cacheDir.getAbsolutePath(),
                dataset -> CacheUtil.cacheFor(
                        dataset.getNamespace(),
                        cacheDir.getAbsolutePath(),
                        inStream -> inStream));


        Dataset actual = registry.datasetFor("local");
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getConfigURI().toString(), endsWith("/local/1cc8eff62af0e6bb3e7771666e2e4109f351b7dfc6fc1dc8314e5671a8eecb80"));
        assertThat(actual.getArchiveURI().toString(), is(localDatasetDir.toURI().toString()));
        assertThat(actual.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature."));

        CacheLocalReadonly readOnlyCache = new CacheLocalReadonly("local", cacheDir.getAbsolutePath(), inStream -> inStream);
        URI localURI = readOnlyCache.getLocalURI(URI.create("https://example.org/data.zip"));

        assertThat(new File(localURI).exists(), is(true));

    }

    @Test(expected = DatasetRegistryException.class)
    public void datasetLocalNoValidAccessFile() throws DatasetRegistryException, URISyntaxException, IOException {
        Path testCacheDir = Files.createTempDirectory(Paths.get("target/"), "test");
        File localDatasetDir = new File(getClass().getResource("/test-dataset-local/globi.json").toURI()).getParentFile();
        File testCacheDirLocal = new File(testCacheDir.toFile(), "local");
        FileUtils.forceMkdir(testCacheDirLocal);
        File accessFile1 = createInvalidProvenanceLog(localDatasetDir, testCacheDirLocal);
        addCachedResources(testCacheDirLocal);

        File cacheDir = new File(accessFile1.toURI()).getParentFile().getParentFile();
        DatasetRegistry registry = new DatasetRegistryLocal(cacheDir.getAbsolutePath(),
                dataset -> CacheUtil.cacheFor(
                        dataset.getNamespace(),
                        cacheDir.getAbsolutePath(),
                        inStream -> inStream));

        Collection<String> availableNamespaces = registry.findNamespaces();
        assertThat(availableNamespaces, not(contains("local")));

        try {
            registry.datasetFor("local");
        } catch (DatasetRegistryException ex) {
            assertThat(ex.getMessage(), is("failed to retrieve/cache dataset in namespace [local]"));
            throw ex;
        }

    }

    public File createLocalCacheDir(Path testCacheDir, File localDatasetDir) throws IOException {
        File testCacheDirLocal = new File(testCacheDir.toFile(), "local");
        FileUtils.forceMkdir(testCacheDirLocal);
        File accessFile = createProvenanceLog(localDatasetDir, testCacheDirLocal);
        addCachedResources(testCacheDirLocal);
        return accessFile;
    }

    public void addCachedResources(File testCacheDirLocal) throws IOException {
        String sha256 = "6bfc17b8717e6e8e478552f12404bc8887d691a155ffd9cd9bfc80cb6747c5d2";
        InputStream resourceAsStream = getClass().getResourceAsStream("/org/globalbioticinteractions/dataset/" + sha256);
        assertNotNull(resourceAsStream);

        FileUtils.copyToFile(
                resourceAsStream,
                new File(testCacheDirLocal, sha256)
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