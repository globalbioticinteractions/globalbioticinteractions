package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.doi.DOI;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class DatasetImporterForBaremore extends NodeBasedImporter {
    private static final URI DATA_SOURCE = URI.create("baremore/ANGELSHARK_DIET_DATAREQUEST_10012012.csv");

    public DatasetImporterForBaremore(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Study study;
        try {
            LabeledCSVParser parser = getParserFactory().createParser(DATA_SOURCE, CharsetConstant.UTF8);
            String[] line;

            study = getNodeFactory().getOrCreateStudy(
                    new StudyImpl("Baremore 2010", new DOI("3354", "ab00214"), ExternalIdUtil.toCitation("Ivy E. Baremore", "Prey Selection By The Atlantic Angel Shark Squatina Dumeril In The Northeastern Gulf Of Mexico.", "2010")));
            Location collectionLocation = getNodeFactory().getOrCreateLocation(new LocationImpl(29.219302, -87.06665, null, null));

            Map<Integer, Specimen> specimenMap = new TreeMap<Integer, Specimen>();

            while ((line = parser.getLine()) != null) {
                Integer sharkId = Integer.parseInt(line[0]);
                String collectionDateString = line[1];
                if (isBlank(collectionDateString)) {
                    getLogger().warn(study, "line [" + parser.getLastLineNumber() + "] in [" + DATA_SOURCE + "]: missing collection date");
                } else {
                    Specimen predatorSpecimen = specimenMap.get(sharkId);
                    if (predatorSpecimen == null) {
                        predatorSpecimen = getNodeFactory().createSpecimen(study, new TaxonImpl("Squatina dumeril", null));
                        predatorSpecimen.caughtIn(collectionLocation);
                        addLifeStage(parser, predatorSpecimen);
                        addCollectionDate(collectionDateString, predatorSpecimen);
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
                        Specimen preySpecimen = getNodeFactory().createSpecimen(study, new TaxonImpl(preySpeciesDescription, null));
                        preySpecimen.caughtIn(collectionLocation);
                        getNodeFactory().setUnixEpochProperty(preySpecimen, getNodeFactory().getUnixEpochProperty(predatorSpecimen));
                        predatorSpecimen.ate(preySpecimen);
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
            List<Term> lifeStages = getNodeFactory().getTermLookupService().lookupTermByName(lifeStageString);
            if (lifeStages.size() == 0) {
                throw new StudyImporterException("unsupported lifeStage [" + lifeStageString + "] on line [" + parser.getLastLineNumber() + "]");
            }
            predatorSpecimen.setLifeStage(lifeStages);
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException("failed ot map life stage string [" + lifeStageString + "]", e);
        }
    }

    private Date addCollectionDate(String dateTime, Specimen specimen) throws NodeFactoryException {
        try {
            Date collectionDate = org.eol.globi.util.DateUtil.parsePatternUTC(dateTime, "MM/dd/yyyy").toDate();
            getNodeFactory().setUnixEpochProperty(specimen, collectionDate);
            return collectionDate;
        } catch (IllegalArgumentException ex) {
            throw new NodeFactoryException("failed to parse [" + dateTime + "]");
        }
    }
}
