package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.globalbioticinteractions.dataset.Dataset;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ParserFactoryForDataset implements ParserFactory {

    private final Dataset dataset;

    public ParserFactoryForDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public LabeledCSVParser createParser(URI studyResource, String characterEncoding) throws IOException {
        InputStream is = dataset.retrieve(studyResource);
        return getLabeledCSVParser(is, characterEncoding);
    }

    public static LabeledCSVParser getLabeledCSVParser(InputStream is, String characterEncoding) throws IOException {
        return CSVTSVUtil.createLabeledCSVParser(FileUtils.getUncompressedBufferedReader(is, characterEncoding));
    }

}
