package org.eol.globi.data;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StudyImporterForArthopodEasyCapture extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForArthopodEasyCapture.class);

    private String rssFeedUrlString;

    public StudyImporterForArthopodEasyCapture(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        final String msgPrefix = "importing archive(s) from [" + rssFeedUrlString + "]";
        LOG.info(msgPrefix + "...");
        final List<StudyImporter> studyImporters = getStudyImportersForRSSFeed(parserFactory, nodeFactory, rssFeedUrlString);
        for (StudyImporter importer : studyImporters) {
            if (importer != null) {
                if (getLogger() != null) {
                    importer.setLogger(getLogger());
                }
                importer.importStudy();
            }
        }
        LOG.info(msgPrefix + " done.");
        return null;
    }

    public void setRssFeedUrlString(String rssFeedUrlString) {
        this.rssFeedUrlString = rssFeedUrlString;
    }

    public String getRssFeedUrlString() {
        return rssFeedUrlString;
    }

    public static List<StudyImporter> getStudyImportersForRSSFeed(ParserFactory parserFactory, NodeFactory
            nodeFactory, String rssUrlString) throws StudyImporterException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed;
        try {
            feed = input.build(new XmlReader(new URL(rssUrlString)));
        } catch (FeedException e) {
            throw new StudyImporterException("failed to read rss feed [" + rssUrlString + "]", e);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read rss feed [" + rssUrlString + "]", e);
        }

        List<StudyImporter> importers = new ArrayList<StudyImporter>();
        final List entries = feed.getEntries();
        for (Object entry : entries) {
            if (entry instanceof SyndEntry) {
                SyndEntry syndEntry = (SyndEntry) entry;
                final StudyImporterForSeltmann studyImporter = new StudyImporterForSeltmann(parserFactory, nodeFactory);
                studyImporter.setArchiveURL(StringUtils.trim(syndEntry.getLink()));
                studyImporter.setSourceCitation(StringUtils.trim(syndEntry.getDescription().getValue()));
                importers.add(studyImporter);
            }
        }
        return importers;
    }

}
