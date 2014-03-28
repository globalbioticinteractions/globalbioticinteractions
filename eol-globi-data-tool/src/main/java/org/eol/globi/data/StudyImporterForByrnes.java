package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.service.UberonLookupService;

import javax.management.relation.RelationType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForByrnes extends BaseStudyImporter {
    public static final String SOURCE = "Southern California Bight Kelp Forest Food Web data provided by Jarrett Byrnes. Also available at http://dx.doi.org/10.1111/j.1365-2486.2011.02409.x";
    public static final String RESOURCE_PATH = "byrnes/supplementary_table_1.csv";
    public static final String REFERENCE_PATH = "byrnes/references.csv";

    public StudyImporterForByrnes(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser dataParser;
        try {
            dataParser = parserFactory.createParser(RESOURCE_PATH, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + RESOURCE_PATH + "]", e);
        }

        Map<String, String> refMap = buildRefMap();

        try {
            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    importLine(dataParser, refMap);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing study at line [" + dataParser.lastLineNumber() + "]", e);
        }
        return null;
    }

    private Map<String, String> buildRefMap() throws StudyImporterException {
        return StudyImporterForBarnes.buildRefMap(parserFactory, REFERENCE_PATH);
    }

    private void importLine(LabeledCSVParser parser, Map<String, String> refMap) throws StudyImporterException {
        Study localStudy = null;
        try {
            String refList = StringUtils.trim(parser.getValueByLabel("Reference"));
            String[] refs = StringUtils.split(refList, ",");
            for (String ref : refs) {
                String singleShortRef = StringUtils.trim(ref);
                if (!refMap.containsKey(singleShortRef)) {
                    throw new StudyImporterException("failed to find ref [" + singleShortRef + "] on line [" + parser.lastLineNumber() + "]");
                }
                localStudy = nodeFactory.getOrCreateStudy("BYRNES-" + singleShortRef, SOURCE, null);
                localStudy.setCitationWithTx(refMap.get(singleShortRef));
                String predatorName = parser.getValueByLabel("Predator");
                if (StringUtils.isBlank(predatorName)) {
                    getLogger().warn(localStudy, "found empty predator name on line [" + parser.lastLineNumber() + "]");
                } else {
                    addInteractionForPredator(parser, localStudy, predatorName);
                }
            }

        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem creating nodes at line [" + parser.lastLineNumber() + "]", e);
        } catch (NumberFormatException e) {
            String message = "skipping record, found malformed field at line [" + parser.lastLineNumber() + "]: ";
            if (localStudy != null) {
                getLogger().warn(localStudy, message + e.getMessage());
            }
        }
    }

    private void addInteractionForPredator(LabeledCSVParser parser, Study localStudy, String predatorName) throws NodeFactoryException, StudyImporterException {
        Specimen predator = nodeFactory.createSpecimen(predatorName);

        String preyName = parser.getValueByLabel("Prey");
        if (StringUtils.isBlank(preyName)) {
            getLogger().warn(localStudy, "found empty prey name on line [" + parser.lastLineNumber() + "]");
        } else {
            Specimen prey = nodeFactory.createSpecimen(preyName);
            String feedingLink = parser.getValueByLabel("Feeding Link?");
            if (StringUtils.equals("1", StringUtils.trim(feedingLink))) {
                predator.ate(prey);
            } else {
                predator.interactsWith(prey, InteractType.INTERACTS_WITH);
            }
        }

        localStudy.collected(predator);
    }

}
