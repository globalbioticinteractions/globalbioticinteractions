package org.eol.globi.data;

import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForZenodoMetadataTest.assertInteractionOfExamplePub;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForZenodoMetadataIT {

    @Test
    public void findAnnotations() throws IOException, StudyImporterException {
        final ResourceService resourceService = TestUtil.getResourceServiceTest();
        final InputStream searchResultStream = resourceService.retrieve(URI.create("https://sandbox.zenodo.org/api/records/?custom=%5Bobo%3ARO_0002453%5D%3A%5B%3A%5D"));
        assertInteractionOfExamplePub(searchResultStream);
    }


    @Test
    public void importStudy() throws IOException, StudyImporterException {

        List<Map<String, String>> links = new ArrayList<>();
        final DatasetImporterForZenodoMetadata importer = new DatasetImporterForZenodoMetadata(null, null);
        importer.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                links.add(link);
            }
        });
        final DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("some:archive"), in -> in) {
            @Override
            public String getOrDefault(String key, String defaultValue) {
                return "https://sandbox.zenodo.org/api/records/?custom=%5Bobo%3ARO_0002453%5D%3A%5B%3A%5D";
            }
        };

        importer.setDataset(dataset);
        importer.importStudy();

        assertThat(links.size() > 0, Is.is(true));
    }


}