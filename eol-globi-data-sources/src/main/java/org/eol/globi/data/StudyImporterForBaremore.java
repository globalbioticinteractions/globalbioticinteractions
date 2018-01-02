package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class StudyImporterForBaremore extends BaseStudyImporter {
    private static final String DATA_SOURCE = "baremore/ANGELSHARK_DIET_DATAREQUEST_10012012.csv";

    public StudyImporterForBaremore(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Study study;
        try {
            LabeledCSVParser parser = parserFactory.createParser(DATA_SOURCE, CharsetConstant.UTF8);
            String[] line;

            study = nodeFactory.getOrCreateStudy(
                    new StudyImpl("Baremore 2010", StudyImporterForGoMexSI2.GOMEXI_SOURCE_DESCRIPTION, "doi:10.3354/ab00214", ExternalIdUtil.toCitation("Ivy E. Baremore", "Prey Selection By The Atlantic Angel Shark Squatina Dumeril In The Northeastern Gulf Of Mexico.", "2010")));
            Location collectionLocation = nodeFactory.getOrCreateLocation(new LocationImpl(29.219302, -87.06665, null, null));

            Map<Integer, Specimen> specimenMap = new HashMap<Integer, Specimen>();

            while ((line = parser.getLine()) != null) {
                Integer sharkId = Integer.parseInt(line[0]);
                String collectionDateString = line[1];
                if (isBlank(collectionDateString)) {
                    getLogger().warn(study, "line [" + parser.getLastLineNumber() + "] in [" + DATA_SOURCE + "]: missing collection date");
                } else {
                    Specimen predatorSpecimen = specimenMap.get(sharkId);
                    if (predatorSpecimen == null) {
                        predatorSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Squatina dumeril", null));
                        predatorSpecimen.caughtIn(collectionLocation);
                        addLifeStage(parser, predatorSpecimen);

                        try {
                            addCollectionDate(collectionDateString, predatorSpecimen);
                        } catch (ParseException ex) {
                            throw new StudyImporterException("failed to parse collection date at line [" + parser.getLastLineNumber() + "] in [" + DATA_SOURCE + "]", ex);
                        }
                    }
                    specimenMap.put(sharkId, predatorSpecimen);

                    String totalLengthInCm = line[3];
                    try {
                        Double lengthInMm = Double.parseDouble(totalLengthInCm) * 10.0;
                        predatorSpecimen.setLengthInMm(lengthInMm);
                    } catch (NumberFormatException ex) {
                        throw new StudyImporterException("failed to parse length [" + totalLengthInCm);
                    }
                    String preySpeciesDescription = line[7];
                    if (StringUtils.isBlank(preySpeciesDescription)) {
                        getLogger().info(study, "found blank prey species description [" + preySpeciesDescription + "] on line [" + parser.lastLineNumber() + "]");
                    } else {
                        Specimen preySpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(preySpeciesDescription, null));
                        preySpecimen.caughtIn(collectionLocation);
                        predatorSpecimen.ate(preySpecimen);
                        nodeFactory.setUnixEpochProperty(preySpecimen, nodeFactory.getUnixEpochProperty(predatorSpecimen));
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to parse labels", e);
        }
    }

    private void addLifeStage(LabeledCSVParser parser, Specimen predatorSpecimen) throws StudyImporterException {
        String lifeStageString = parser.getValueByLabel("Mat State");
        try {
            List<Term> lifeStages = nodeFactory.getTermLookupService().lookupTermByName(lifeStageString);
            if (lifeStages.size() == 0) {
                throw new StudyImporterException("unsupported lifeStage [" + lifeStageString + "] on line [" + parser.getLastLineNumber() + "]");
            }
            predatorSpecimen.setLifeStage(lifeStages);
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException("failed ot map life stage string [" + lifeStageString + "]", e);
        }
    }

    private Date addCollectionDate(String s, Specimen specimen) throws ParseException, NodeFactoryException {
        Date collectionDate = new SimpleDateFormat("MM/dd/yyyy").parse(s);
        nodeFactory.setUnixEpochProperty(specimen, collectionDate);
        return collectionDate;
    }
}
