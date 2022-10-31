package org.globalbioticinteractions.dataset;

import org.eol.globi.util.ResourceServiceHTTP;
import org.junit.Test;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetRegistryGitHubIT {

    @Test
    public void discoverDatasetsInGitHub() throws DatasetRegistryException {
        AtomicBoolean usedStreamFactory = new AtomicBoolean(false);
        Iterable<String> urls = new DatasetRegistryGitHubArchive(new ResourceServiceHTTP(inStream -> {
            usedStreamFactory.set(true);
            return inStream;
        })).findNamespaces();


        assertThat(urls.iterator().hasNext(), is(true));
        assertThat(usedStreamFactory.get(), is(true));
    }

    @Test
    public void datasetFor() throws DatasetRegistryException {
        URI uri = new DatasetRegistryGitHubArchive(new ResourceServiceHTTP(inStream -> inStream)).datasetFor("globalbioticinteractions/template-dataset").getArchiveURI();
        assertThat(uri.toString(), startsWith("https://github.com/globalbioticinteractions/template-dataset/archive/"));
        assertThat(uri.toString(), endsWith(".zip"));
    }

}