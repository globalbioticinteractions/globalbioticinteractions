package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Study;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForRaymond extends BaseStudyImporter {

    private final static Log LOG = LogFactory.getLog(StudyImporter.class);

    public StudyImporterForRaymond(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            File dietFile = download("diet", "http://esapubs.org/archive/ecol/E092/097/diet.csv");
            File sources = download("sources", "http://esapubs.org/archive/ecol/E092/097/sources.csv");

            LabeledCSVParser sourcesParser = new LabeledCSVParser(new CSVParser(new FileInputStream(sources)));
            LabeledCSVParser dietParser = new LabeledCSVParser(new CSVParser(new FileInputStream(dietFile)));

            importData(sourcesParser, dietParser);

        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + getClass().getSimpleName() + "]", e);
        }
        return null;
    }

    public void importData(LabeledCSVParser sourcesParser, LabeledCSVParser dietParser) throws IOException {
        Map<Integer, String> sourceMap = new HashMap<Integer, String>();
        while (sourcesParser.getLine() != null) {
            Integer sourceId = Integer.parseInt(sourcesParser.getValueByLabel("SOURCE_ID"));
            String reference = sourcesParser.getValueByLabel("DETAILS");
            sourceMap.put(sourceId, reference);
        }

        while (dietParser.getLine() != null) {
            dietParser.getValueByLabel("SOURCE_ID");
            dietParser.getValueByLabel("PREDATOR_NAME");
            dietParser.getValueByLabel("PREDATOR_LIFE_STAGE");
            dietParser.getValueByLabel("PREDATOR_SEX");

            dietParser.getValueByLabel("OBSERVATION_DATE_START");
            dietParser.getValueByLabel("OBSERVATION_DATE_END");

            dietParser.getValueByLabel("ALTITUDE_MIN");
            dietParser.getValueByLabel("ALTITUDE_MAX");

            dietParser.getValueByLabel("DEPTH_MIN");
            dietParser.getValueByLabel("DEPTH_MAX");

            dietParser.getValueByLabel("WEST");
            dietParser.getValueByLabel("NORTH");

            dietParser.getValueByLabel("EAST");
            dietParser.getValueByLabel("SOUTH");

            dietParser.getValueByLabel("PREY_NAME");
            dietParser.getValueByLabel("PREY_LIFE_STAGE");
        }
    }

    private File download(String prefix, String dataUrl) throws StudyImporterException {
        try {
            File tmpFile = File.createTempFile(prefix, ".csv");
            FileOutputStream os = new FileOutputStream(tmpFile);
            LOG.info("[" + tmpFile.getAbsolutePath() + "] downloading...");
            InputStream is = new URL(dataUrl).openStream();
            IOUtils.copy(is, os);
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            if (tmpFile.exists()) {
                LOG.info("[" + tmpFile.getAbsolutePath() + "] downloaded.");
            }
            return tmpFile;
        } catch (IOException e) {
            throw new StudyImporterException("failed to donwload [" + dataUrl + "]", e);
        }
    }
}
