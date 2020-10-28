package org.globalbioticinteractions.dataset;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetFactoryTest {

    @Test
    public void createDataset() throws DatasetRegistryException, URISyntaxException {
        String meta = "jar:" + getClass().getResource("/org/globalbioticinteractions/dataset/archive.zip").toURI().toString() + "!/template-dataset-e68f4487ebc3bc70668c0f738223b92da0598c00/";
        Dataset dataset = datasetFor(meta);

        assertThat(dataset.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature."));

    }

    private Dataset datasetFor(String meta) throws DatasetRegistryException {
        final DatasetRegistry finder = new DatasetRegistry() {
            @Override
            public Collection<String> findNamespaces() throws DatasetRegistryException {
                return Collections.singletonList("some/repo");
            }

            @Override
            public Dataset datasetFor(String namespace) throws DatasetRegistryException {
                return new DatasetImpl(namespace, URI.create(meta), inStream -> inStream);
            }
        };
        return new DatasetFactory(finder).datasetFor("some/repo");
    }

    @Test
    public void createDatasetDWCA() throws DatasetRegistryException, URISyntaxException {
        String meta = "jar:" + getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip").toURI().toString() + "!/vampire-moth-dwca-c4549a1690b84595c88946f477057b9ab76e5360";
        Dataset dataset = datasetFor(meta);
        assertThat(dataset.getCitation(), is("Occurrence Records for vampire-moths-and-their-fruit-piercing-relatives. 2018-09-27. South Central California Network - 2ba077c1-aa41-455e-9a84-bccb61a91230."));

    }

}