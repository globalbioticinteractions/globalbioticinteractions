package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForRegistryTest {

    @Test(expected = StudyImporterException.class)
    public void throwOnMissingConfig() throws StudyImporterException {
        DatasetImporterForRegistry importer = new DatasetImporterForRegistry(null, null, new DatasetRegistry() {
            @Override
            public Iterable<String> findNamespaces() throws DatasetRegistryException {
                return Collections.singletonList("some/namespace");
            }

            @Override
            public Dataset datasetFor(String namespace) throws DatasetRegistryException {
                return new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(in -> in));
            }
        });

        try {
            importer.importStudy();
        } catch (StudyImporterException ex) {
            assertThat(ex.getMessage(), Is.is("failed to import one or more repositories: [some/namespace]"));
            throw ex;
        }
    }

    @Test
    public void filteredDatasets() throws StudyImporterException {
        DatasetImporterForRegistry importer = new DatasetImporterForRegistry(
                null,
                null,
                new DatasetRegistry() {
                    @Override
                    public Iterable<String> findNamespaces() throws DatasetRegistryException {
                        return Collections.singletonList("some/namespace");
                    }

                    @Override
                    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
                        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(in -> in)) {
                            @Override
                            public InputStream retrieve(URI resource) throws IOException {
                                if (!StringUtils.endsWith(resource.toString(), "globi.json")) {
                                    throw new IOException();
                                }
                                return IOUtils.toInputStream("{\"some\":\"thing\"}", StandardCharsets.UTF_8);
                            }
                        };
                        return dataset;
                    }
                });

        importer.setDatasetFilter(x -> false);
        importer.importStudy();
    }

}