package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.util.logging.Level;

public class StudyImporterForJRFerrerParis extends BaseStudyImporter {

    private static final String RESOURCE = "http://files.figshare.com/1674327/20140912_CompiledButterflyHostPlantRecords_JRFP.csv";


    public StudyImporterForJRFerrerParis(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        String citation = "Ferrer-Paris, José R.; Sánchez-Mercado, Ada Y.; Lozano, Cecilia; Zambrano, Liset; Soto, José; Baettig, Jessica; Leal, María (2014): A compilation of larval host-plant records for six families of butterflies (Lepidoptera: Papilionoidea) from available electronic resources. figshare. https://doi.org/10.6084/m9.figshare.1168861 . " + CitationUtil.createLastAccessedString(RESOURCE);
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("Ferrer-Paris 2014", citation, "https://doi.org/10.6084/m9.figshare.1168861", citation));
        try {
            LabeledCSVParser parser = parserFactory.createParser(RESOURCE, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String butterflyName = createTaxon(parser, "Lepidoptera Name");
                String plantName = createTaxon(parser, "Hostplant Name");
                if (validNames(butterflyName, plantName, study, parser.lastLineNumber())) {
                    addAssociation(study, parser, butterflyName, plantName);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource [" + RESOURCE + "]");
        }
    }

    private void addAssociation(Study study, LabeledCSVParser parser, String butterflyName, String plantName) throws StudyImporterException {
        Specimen instigatorSpecimen;
        try {
            instigatorSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(butterflyName, null));
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create butterfly specimen [" + butterflyName + "] on line [" + parser.lastLineNumber() + "]", e);
        }
        Specimen targetSpecimen;
        try {
            targetSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(plantName, null));
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
