package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;

public class StudyImporterForThessen extends BaseStudyImporter {

    public static final String RESOURCE = "https://raw.github.com/EOL/pseudonitzchia/master/associations_all_revised.txt";

    public StudyImporterForThessen(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy("Thessen 2014", RESOURCE, null);
        study.setCitationWithTx("A. Thessen. 2014 Accessed at " + RESOURCE + " .");
        HttpGet httpGet = new HttpGet(RESOURCE);
        try {
            HttpResponse response = HttpUtil.createHttpClient().execute(httpGet);
            CSVParser parser = new CSVParser(response.getEntity().getContent(), '\t');
            String[] line;
            while (parser.getLine() != null) {
                if (importFilter.shouldImportRecord((long)parser.lastLineNumber())) {
                    line = parser.getLine();
                    if (line.length == 2) {
                        try {
                            Specimen source = nodeFactory.createSpecimen("some name", "EOL:" + line[0]);
                            Specimen target = nodeFactory.createSpecimen("some name", "EOL:" + line[1]);
                            source.interactsWith(target, InteractType.INTERACTS_WITH);
                            study.collected(source);
                        } catch (NodeFactoryException e) {
                            throw new StudyImporterException("failed to create nodes on line [" + parser.getLastLineNumber() + "]");
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access [" + RESOURCE + "]");
        }
        return study;
    }
}
