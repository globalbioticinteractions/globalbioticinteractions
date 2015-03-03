package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;

public class StudyImporterForThessen extends BaseStudyImporter {

    public static final String RESOURCE = "https://raw.githubusercontent.com/EOL/pseudonitzchia/master/associations_all_revised.txt";

    public StudyImporterForThessen(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        String citation = "A. Thessen. 2014. Species associations extracted from EOL text data objects via text mining. " + ReferenceUtil.createLastAccessedString(RESOURCE);
        Study study = nodeFactory.getOrCreateStudy2("Thessen 2014", citation, null);
        study.setExternalId("https://github.com/EOL/pseudonitzchia");
        study.setCitationWithTx(citation);
        try {
            LabeledCSVParser parser = parserFactory.createParser(RESOURCE, "UTF-8");
            parser.changeDelimiter('\t');
            String[] line;
            while ((line = parser.getLine()) != null) {
                if (importFilter.shouldImportRecord((long)parser.lastLineNumber())) {
                    if (line.length == 2) {
                        try {
                            Specimen source = nodeFactory.createSpecimen(study, null, "EOL:" + line[0]);
                            Specimen target = nodeFactory.createSpecimen(study, null, "EOL:" + line[1]);
                            source.interactsWith(target, InteractType.INTERACTS_WITH);
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
