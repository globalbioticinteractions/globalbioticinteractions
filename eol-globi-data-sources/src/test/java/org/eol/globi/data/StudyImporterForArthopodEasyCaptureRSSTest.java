package org.eol.globi.data;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetRemote;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForArthopodEasyCaptureRSSTest {

    @Test
    public void readRSS() throws StudyImporterException {
        final ParserFactory parserFactory = null;
        final NodeFactory nodeFactory = null;
        final String rssUrlString = "http://amnh.begoniasociety.org/dwc/rss.xml";

        final Dataset dataset = new DatasetRemote("some/namespace", URI.create("http://example.com"));

        List<StudyImporter> importers = StudyImporterForArthopodEasyCapture.getStudyImportersForRSSFeed(dataset, parserFactory, nodeFactory, rssUrlString);

        assertThat(importers.size(), is(3));

    }

}