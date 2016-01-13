package org.eol.globi.data;

import com.sun.syndication.io.FeedException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForArthopodEasyCaptureRSSTest {

    @Test
    public void readRSS() throws StudyImporterException {
        final ParserFactory parserFactory = null;
        final NodeFactory nodeFactory = null;
        final String rssUrlString = "http://amnh.begoniasociety.org/dwc/rss.xml";

        List<StudyImporter> importers = StudyImporterForArthopodEasyCapture.getStudyImportersForRSSFeed(parserFactory, nodeFactory, rssUrlString);

        assertThat(importers.size(), is(3));

    }

}