package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.util.logging.Level;

public class StudyImporterForJRFerrerParis extends BaseStudyImporter {

    public StudyImporterForJRFerrerParis(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy("JRFerrisParisButterflies",
                "Jose R. Ferrer Paris",
                "",
                "",
                "Ferrer-Paris JR, Sánchez-Mercado AY, Lozano C, Zambrano L, Soto J, Baettig J, Ortega P, Leal M. Using web-content for the assessment of macroecological patterns in butterfly-hostplant associations at a global scale. March 2013 - January 2014. Unpublished results."
                , null
                , "http://papilionoidea.myspecies.info/");
        String studyResource = "jr_ferrer_paris/CompiledButterflyHostPlantRecords_JRFP.csv";
        try {
            LabeledCSVParser parser = parserFactory.createParser(studyResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String butterflyName = createTaxon(parser, "Lepidoptera Name");
                String plantName = createTaxon(parser, "Hostplant Name");
                if (validNames(butterflyName, plantName, study, parser.lastLineNumber())) {
                    addAssociation(study, parser, butterflyName, plantName);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource [" + studyResource + "]");
        }

        return study;
    }

    private void addAssociation(Study study, LabeledCSVParser parser, String butterflyName, String plantName) throws StudyImporterException {
        Specimen instigatorSpecimen;
        try {
            instigatorSpecimen = nodeFactory.createSpecimen(butterflyName);
            study.collected(instigatorSpecimen);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create butterfly specimen [" + butterflyName + "] on line [" + parser.lastLineNumber() + "]", e);
        }
        Specimen targetSpecimen;
        try {
            targetSpecimen = nodeFactory.createSpecimen(plantName);
            instigatorSpecimen.ate(targetSpecimen);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to associate butterfly [" + butterflyName + "] to plant [" + plantName + "] on line [" + parser.lastLineNumber() + "]", e);
        }
    }

    private boolean validNames(String butterflyName, String plantName, Study study, int lineNumber) {
        boolean isValid = false;
        if (StringUtils.length(butterflyName) < 3) {
            study.appendLogMessage("butterfly name [" + butterflyName + "] on line [" + lineNumber + "] too short: skipping association", Level.WARNING);
        } else if (StringUtils.length(plantName) < 3) {
            study.appendLogMessage("plant name [" + plantName + "] on line [" + lineNumber + "] too short: skipping association", Level.WARNING);
        } else {
            isValid = true;
        }
        return isValid;
    }


    private String createTaxon(LabeledCSVParser parser, String taxonLabel) throws StudyImporterException {
        String instigatorScientificName = parser.getValueByLabel(taxonLabel);
        if (StringUtils.isBlank(instigatorScientificName)) {
            throw new StudyImporterException("found missing instigator scientific name at line [" + parser.getLastLineNumber() + "]");
        } else {
            return StringUtils.trim(instigatorScientificName);
        }
    }
}
