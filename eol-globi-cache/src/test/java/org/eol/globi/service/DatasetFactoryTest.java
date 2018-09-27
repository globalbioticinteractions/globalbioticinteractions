package org.eol.globi.service;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DatasetFactoryTest {

    @Test
    public void createDataset() throws DatasetFinderException, URISyntaxException {
        String meta = "jar:" + getClass().getResource("/org/globalbioticinteractions/dataset/archive.zip").toURI().toString() + "!/template-dataset-e68f4487ebc3bc70668c0f738223b92da0598c00/";
        Dataset dataset = datasetFor(meta);

        assertThat(dataset.getCitation(), is("Jorrit H. Poelen. 2014. Species associations manually extracted from literature."));

    }

    private Dataset datasetFor(String meta) throws DatasetFinderException {
        final DatasetFinder finder = new DatasetFinder() {
            @Override
            public Collection<String> findNamespaces() throws DatasetFinderException {
                return Collections.singletonList("some/repo");
            }

            @Override
            public Dataset datasetFor(String namespace) throws DatasetFinderException {
                return new DatasetImpl(namespace, URI.create(meta));
            }
        };
        return DatasetFactory.datasetFor("some/repo", finder);
    }

    @Test
    public void createDatasetDWCA() throws DatasetFinderException, URISyntaxException {
        String meta = "jar:" + getClass().getResource("/org/globalbioticinteractions/dataset/dwca.zip").toURI().toString() + "!/vampire-moth-dwca-master";
        Dataset dataset = datasetFor(meta);
        assertThat(dataset.getCitation(), is("Occurrence Records for vampire-moths-and-their-fruit-piercing-relatives. 2018-09-18. South Central California Network - 5f573b1a-0e9a-43cf-95d7-299207f98522."));

    }

}