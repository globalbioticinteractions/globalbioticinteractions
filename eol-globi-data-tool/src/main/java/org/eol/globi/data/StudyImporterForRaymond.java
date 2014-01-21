package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
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

    public static final String DIET_DATA_URL = "http://esapubs.org/archive/ecol/E092/097/diet.csv";

    public StudyImporterForRaymond(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            File dietFile = download("diet", "http://esapubs.org/archive/ecol/E092/097/diet.csv");
            File sources = download("sources", "http://esapubs.org/archive/ecol/E092/097/sources.csv");

            LabeledCSVParser sourcesParser = new LabeledCSVParser(new CSVParser(new FileInputStream(sources)));
            Map<Integer, String> sourceMap = new HashMap<Integer, String>();
            while (sourcesParser.getLine() != null) {
                Integer sourceId = Integer.parseInt(sourcesParser.getValueByLabel("SOURCE_ID"));
                String reference = sourcesParser.getValueByLabel("DETAILS");
                sourceMap.put(sourceId, reference);
            }

            LabeledCSVParser dietParser = new LabeledCSVParser(new CSVParser(new FileInputStream(dietFile)));
            while(dietParser.getLine() != null) {
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
                dietParser.getValueByLabel("EAST");
                dietParser.getValueByLabel("SOUTH");
                dietParser.getValueByLabel("NORTH");

                dietParser.getValueByLabel("PREY_NAME");
                dietParser.getValueByLabel("PREY_LIFE_STAGE");
            }


        } catch (IOException e) {
            throw new StudyImporterException("faile to retreive [" + DIET_DATA_URL + "]", e);
        }


        return null;
    }

    private File download(String prefix, String dataUrl) throws IOException {
        File tmpFile = File.createTempFile(prefix, ".csv");
        FileOutputStream os = new FileOutputStream(tmpFile);
        System.out.print("[" + tmpFile.getAbsolutePath() + "] downloading...");
        InputStream is = new URL(dataUrl).openStream();
        IOUtils.copy(is, os);
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
        if (tmpFile.exists()) {
            System.out.print("[" + tmpFile.getAbsolutePath() + "] downloaded.");
        }
        return tmpFile;
    }
}
