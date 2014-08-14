package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StudyImporterForThessen extends BaseStudyImporter {

    private static final Log LOG = LogFactory.getLog(StudyImporterForThessen.class);

    public static final String RESOURCE = "https://raw.github.com/EOL/pseudonitzchia/master/associations_all_revised.txt";

    public StudyImporterForThessen(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        String citation = "A. Thessen. 2014. Species associations extracted from EOL text data objects via text mining. Accessed at " + RESOURCE + " .";
        Study study = nodeFactory.getOrCreateStudy("Thessen 2014", citation, null);
        study.setCitationWithTx(citation);
        try {
            LabeledCSVParser parser = parserFactory.createParser(RESOURCE, "UTF-8");
            parser.changeDelimiter('\t');
            String[] line;
            while ((line = parser.getLine()) != null) {
                if (importFilter.shouldImportRecord((long)parser.lastLineNumber())) {
                    if (line.length == 2) {
                        try {
                            Specimen source = nodeFactory.createSpecimen(null, "EOL:" + line[0]);
                            Specimen target = nodeFactory.createSpecimen(null, "EOL:" + line[1]);
                            source.interactsWith(target, InteractType.INTERACTS_WITH);
                            study.collected(source);
                        } catch (NodeFactoryException e) {
                            throw new StudyImporterException("failed to create nodes on line [" + parser.getLastLineNumber() + "]", e);
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new StudyImporterException("failed to access [" + RESOURCE + "]", e);
        }
        return study;
    }

}
