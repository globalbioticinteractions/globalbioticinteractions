package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;

import java.io.IOException;

public class StudyImporterForThessen extends BaseStudyImporter {

    public static final String RESOURCE = "https://raw.githubusercontent.com/EOL/pseudonitzchia/master/associations_all_revised.txt";

    public StudyImporterForThessen(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        String citation = "A. Thessen. 2014. Species associations extracted from EOL text data objects via text mining. " + ReferenceUtil.createLastAccessedString(RESOURCE);
        StudyImpl study1 = new StudyImpl("Thessen 2014", citation, null, citation);
        study1.setExternalId("https://github.com/EOL/pseudonitzchia");
        Study study = nodeFactory.getOrCreateStudy(study1);
        try {
            LabeledCSVParser parser = parserFactory.createParser(RESOURCE, "UTF-8");
            parser.changeDelimiter('\t');
            String[] line;
            while ((line = parser.getLine()) != null) {
                if (importFilter.shouldImportRecord((long)parser.lastLineNumber())) {
                    if (line.length == 2) {
                        try {
                            Specimen source = nodeFactory.createSpecimen(study, new TaxonImpl(null, "EOL:" + line[0]));
                            Specimen target = nodeFactory.createSpecimen(study, new TaxonImpl(null, "EOL:" + line[1]));
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
