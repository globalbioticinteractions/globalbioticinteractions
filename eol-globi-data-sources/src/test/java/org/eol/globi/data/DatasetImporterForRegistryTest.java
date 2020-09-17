package org.eol.globi.data;

import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForRegistryTest {

    @Test(expected = StudyImporterException.class)
    public void throwOnMissingConfig() throws StudyImporterException {
        DatasetImporterForRegistry importer = new DatasetImporterForRegistry(null, null, new DatasetRegistry() {
            @Override
            public Collection<String> findNamespaces() throws DatasetRegistryException {
                return Collections.singletonList("some/namespace");
            }

            @Override
            public Dataset datasetFor(String namespace) throws DatasetRegistryException {
                return new DatasetImpl("some/namespace", URI.create("some:uri"), in -> in);
            }
        });

        try {
            importer.importStudy();
        } catch(StudyImporterException ex) {
            assertThat(ex.getMessage(), Is.is("failed to import one or more repositories: [some/namespace]"));
            throw ex;
        }
    }



}