package org.eol.globi.service;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DatasetFactoryTest {

    @Test
    public void createDataset() throws DatasetFinderException {
        final DatasetFinder finder = new DatasetFinder() {
            @Override
            public Collection<String> findNamespaces() throws DatasetFinderException {
                return Arrays.asList("some/repo");
            }

            @Override
            public Dataset datasetFor(String namespace) throws DatasetFinderException {
                try {
                    return new DatasetImpl(namespace, URI.create("jar:" + getClass().getResource("/org/globalbioticinteractions/dataset/archive.zip").toURI().toString() + "!/template-dataset-e68f4487ebc3bc70668c0f738223b92da0598c00/"));
                } catch (URISyntaxException e) {
                    throw new DatasetFinderException(e);
                }
            }
        };
        Dataset dataset = DatasetFactory.datasetFor("some/repo", finder);

        assertThat(dataset.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature."));

    }

}